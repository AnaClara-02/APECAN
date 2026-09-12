package com.aclg.apecan.backup.service;

import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.backup.entity.ResultadoBackup;
import com.aclg.apecan.shared.config.AuditoriaConfig;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class BackupService {

	private static final DateTimeFormatter NOME_ARQUIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");
	private final ReentrantLock exclusaoMutua = new ReentrantLock();
	private final UsuarioAtual usuarioAtual;
	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final PostgreSqlBackupClient postgreSql;
	private final BackupCriptografiaService criptografia;
	private final ChecksumService checksumService;
	private final VersoesBackupService versoes;
	private final BackupAuditoriaService auditoria;
	private final Clock clock;

	public BackupService(UsuarioAtual usuarioAtual, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			PostgreSqlBackupClient postgreSql, BackupCriptografiaService criptografia, ChecksumService checksumService,
			VersoesBackupService versoes, BackupAuditoriaService auditoria, Clock clock) {
		this.usuarioAtual = usuarioAtual;
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.postgreSql = postgreSql;
		this.criptografia = criptografia;
		this.checksumService = checksumService;
		this.versoes = versoes;
		this.auditoria = auditoria;
		this.clock = clock;
	}

	public BackupArquivo exportar(String senhaAtual, String senhaBackup) {
		Usuario responsavel = usuarioRepository.findById(usuarioAtual.exigirId())
			.orElseThrow(() -> new OperacaoInvalidaException("USUARIO_NAO_ENCONTRADO", "Usuário autenticado inválido."));
		if (!responsavel.estaAtivado() || !passwordEncoder.matches(senhaAtual, responsavel.getSenhaHash())) {
			throw new OperacaoInvalidaException("SENHA_ATUAL_INVALIDA", "A senha atual não confere.");
		}
		if (!exclusaoMutua.tryLock()) {
			throw new OperacaoInvalidaException("BACKUP_EM_ANDAMENTO", "Já existe uma exportação de backup em andamento.");
		}
		Path trabalho = null;
		char[] segredo = senhaBackup.toCharArray();
		try {
			trabalho = criptografia.criarDiretorioTemporario("apecan-exportacao-");
			Path dump = trabalho.resolve("database.dump");
			postgreSql.exportar(dump);
			String checksumDump = checksumService.sha256(dump);
			LocalDateTime agora = LocalDateTime.now(clock);
			BackupManifest manifesto = new BackupManifest(1, versoes.versaoAplicacao(), versoes.versaoEsquemaAtual(),
				versoes.versaoPostgreSql(), agora, AuditoriaConfig.FUSO_HORARIO_APECAN.getId(),
				Map.of("database.dump", checksumDump), List.of("BANCO_POSTGRESQL"));
			Path arquivo = criptografia.empacotar(dump, manifesto, segredo, trabalho);
			ArquivosTemporarios.apagarSilenciosamente(dump);
			String checksumArquivo = checksumService.sha256(arquivo);
			auditoria.registrarExportacao(ResultadoBackup.SUCESSO, manifesto.versaoEsquema(), checksumArquivo, null,
				responsavel);
			return new BackupArquivo(arquivo, "apecan-backup-" + agora.format(NOME_ARQUIVO) + ".apecan-backup",
				checksumArquivo);
		}
		catch (RuntimeException exception) {
			String codigo = exception instanceof RegraNegocioException regra ? regra.getCodigo() : "FALHA_BACKUP";
			try {
				auditoria.registrarExportacao(ResultadoBackup.FALHA, versoes.versaoEsquemaAtual(), null, codigo,
					responsavel);
			}
			catch (RuntimeException ignored) {
				// A falha da auditoria não substitui a causa segura apresentada ao administrador.
			}
			ArquivosTemporarios.apagarRecursivamente(trabalho);
			throw exception;
		}
		finally {
			Arrays.fill(segredo, '\0');
			exclusaoMutua.unlock();
		}
	}

	public void limpar(BackupArquivo arquivo) {
		if (arquivo != null && arquivo.caminho() != null) {
			ArquivosTemporarios.apagarRecursivamente(arquivo.caminho().getParent());
		}
	}
}
