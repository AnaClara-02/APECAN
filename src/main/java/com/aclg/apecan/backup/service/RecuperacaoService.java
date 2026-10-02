package com.aclg.apecan.backup.service;

import com.aclg.apecan.auth.security.SessaoUsuarioService;
import com.aclg.apecan.backup.config.BackupProperties;
import com.aclg.apecan.backup.entity.ResultadoBackup;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class RecuperacaoService {

	private final ReentrantLock exclusaoMutua = new ReentrantLock();
	private final BackupProperties properties;
	private final BackupCriptografiaService criptografia;
	private final PostgreSqlBackupClient postgreSql;
	private final VersoesBackupService versoes;
	private final BackupAuditoriaService auditoria;
	private final UsuarioRepository usuarioRepository;
	private final JdbcTemplate jdbcTemplate;
	private final ObjectProvider<Flyway> flywayProvider;
	private final SessaoUsuarioService sessaoUsuarioService;
	private final RecuperacaoEncerramentoService encerramentoService;
	private final ChecksumService checksumService;
	private final Clock clock;

	public RecuperacaoService(BackupProperties properties, BackupCriptografiaService criptografia,
			PostgreSqlBackupClient postgreSql, VersoesBackupService versoes, BackupAuditoriaService auditoria,
			UsuarioRepository usuarioRepository, JdbcTemplate jdbcTemplate, ObjectProvider<Flyway> flywayProvider,
			SessaoUsuarioService sessaoUsuarioService, RecuperacaoEncerramentoService encerramentoService,
			ChecksumService checksumService, Clock clock) {
		this.properties = properties;
		this.criptografia = criptografia;
		this.postgreSql = postgreSql;
		this.versoes = versoes;
		this.auditoria = auditoria;
		this.usuarioRepository = usuarioRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.flywayProvider = flywayProvider;
		this.sessaoUsuarioService = sessaoUsuarioService;
		this.encerramentoService = encerramentoService;
		this.checksumService = checksumService;
		this.clock = clock;
	}

	public ResultadoRestauracao restaurar(MultipartFile upload, String senhaBackup) {
		validarUpload(upload);
		if (!exclusaoMutua.tryLock()) {
			throw new OperacaoInvalidaException("RESTAURACAO_EM_ANDAMENTO", "Já existe uma restauração em andamento.");
		}
		Path trabalho = null;
		Path seguranca = null;
		char[] segredo = senhaBackup.toCharArray();
		BackupManifest manifesto = null;
		String checksumArquivo = null;
		boolean bancoAlterado = false;
		boolean preservarSeguranca = false;
		try {
			trabalho = criptografia.criarDiretorioTemporario("apecan-upload-");
			Path arquivo = trabalho.resolve("entrada.apecan-backup");
			copiarUpload(upload, arquivo);
			validarEspaco(arquivo);
			checksumArquivo = checksumService.sha256(arquivo);
			try (BackupExtraido extraido = criptografia.abrir(arquivo, segredo)) {
				manifesto = extraido.manifesto();
				if (!versoes.suporta(manifesto.versaoEsquema())) {
					throw new OperacaoInvalidaException("VERSAO_BACKUP_INCOMPATIVEL",
						"Este backup foi criado por uma versão mais nova. Atualize o APECAN antes de restaurar.");
				}
				// Mesmo uma instalacao sem usuarios possui esquema/Flyway que precisa de reversao.
				seguranca = trabalho.resolve("estado-anterior.dump");
				postgreSql.exportar(seguranca);
				postgreSql.restaurar(extraido.dump());
				bancoAlterado = true;
				migrar();
				invalidarTokensPendentes();
				auditoria.registrarRestauracao(ResultadoBackup.SUCESSO, manifesto.versaoEsquema(), checksumArquivo, null);
				sessaoUsuarioService.encerrarTodasSessoes();
				boolean reinicio = encerramentoService.agendarSeConfigurado();
				return new ResultadoRestauracao(manifesto.versaoEsquema(), LocalDateTime.now(clock), reinicio);
			}
		}
		catch (RuntimeException exception) {
			String codigo = exception instanceof RegraNegocioException regra ? regra.getCodigo() : "FALHA_RESTAURACAO";
			if (bancoAlterado && seguranca != null && Files.isRegularFile(seguranca)) {
				preservarSeguranca = !tentarReverter(seguranca);
			}
			tentarAuditarFalha(manifesto, checksumArquivo, codigo);
			if (preservarSeguranca) {
				throw new OperacaoInvalidaException("REVERSAO_INCOMPLETA",
					"A recuperação e a reversão falharam. Mantenha o sistema parado. A cópia técnica foi preservada na área protegida de backup.");
			}
			throw exception;
		}
		finally {
			Arrays.fill(segredo, '\0');
			if (!preservarSeguranca) ArquivosTemporarios.apagarRecursivamente(trabalho);
			exclusaoMutua.unlock();
		}
	}

	private void validarUpload(MultipartFile upload) {
		if (upload == null || upload.isEmpty()) {
			throw new OperacaoInvalidaException("ARQUIVO_BACKUP_OBRIGATORIO", "Selecione um arquivo de backup.");
		}
		if (upload.getSize() > properties.getTamanhoMaximo().toBytes()) {
			throw new OperacaoInvalidaException("BACKUP_MUITO_GRANDE", "O arquivo ultrapassa o tamanho máximo permitido.");
		}
	}

	private void copiarUpload(MultipartFile upload, Path destino) {
		long limite = properties.getTamanhoMaximo().toBytes();
		long total = 0;
		try (InputStream entrada = upload.getInputStream(); OutputStream saida = Files.newOutputStream(destino)) {
			byte[] buffer = new byte[8192];
			int lidos;
			while ((lidos = entrada.read(buffer)) >= 0) {
				total += lidos;
				if (total > limite) {
					throw new OperacaoInvalidaException("BACKUP_MUITO_GRANDE",
						"O arquivo ultrapassa o tamanho máximo permitido.");
				}
				saida.write(buffer, 0, lidos);
			}
		}
		catch (IOException exception) {
			throw new OperacaoInvalidaException("FALHA_UPLOAD_BACKUP", "Não foi possível receber o arquivo de backup.");
		}
	}

	private void validarEspaco(Path arquivo) {
		try {
			FileStore disco = Files.getFileStore(arquivo);
			long necessario = Math.multiplyExact(Files.size(arquivo), 3L);
			if (disco.getUsableSpace() < necessario) {
				throw new OperacaoInvalidaException("ESPACO_INSUFICIENTE",
					"Não há espaço livre suficiente para validar e restaurar o backup.");
			}
		}
		catch (ArithmeticException | IOException exception) {
			throw new OperacaoInvalidaException("ESPACO_INDISPONIVEL",
				"Não foi possível confirmar o espaço necessário para a restauração.");
		}
	}

	private void invalidarTokensPendentes() {
		jdbcTemplate.update("""
				UPDATE tokens_credencial
				SET utilizado_em = CURRENT_TIMESTAMP
				WHERE utilizado_em IS NULL
				""");
	}

	private boolean tentarReverter(Path seguranca) {
		try {
			postgreSql.restaurar(seguranca);
			migrar();
			return true;
		}
		catch (RuntimeException ignored) {
			return false;
		}
	}

	private void migrar() {
		Flyway flyway = flywayProvider.getIfAvailable();
		if (flyway == null) {
			throw new OperacaoInvalidaException("FLYWAY_INDISPONIVEL",
				"A recuperação exige o Flyway ativo para validar o banco restaurado.");
		}
		flyway.migrate();
	}

	private void tentarAuditarFalha(BackupManifest manifesto, String checksum, String codigo) {
		try {
			auditoria.registrarRestauracao(ResultadoBackup.FALHA,
				manifesto == null ? null : manifesto.versaoEsquema(), checksum, codigo);
		}
		catch (RuntimeException ignored) {
			// Pode não haver esquema disponível durante uma falha grave de recuperação.
		}
	}
}
