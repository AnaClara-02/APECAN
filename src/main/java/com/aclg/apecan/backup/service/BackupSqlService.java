package com.aclg.apecan.backup.service;

import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.auth.security.ReautenticacaoService;
import com.aclg.apecan.backup.config.BackupProperties;
import com.aclg.apecan.backup.entity.ResultadoBackup;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BackupSqlService {

	private static final String FORMATO = "1";
	private static final DateTimeFormatter NOME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");
	private static final Pattern INSERT = Pattern.compile("INSERT INTO ([a-z_]+) \\(([^)]+)\\) VALUES \\((.*)\\);");
	private static final Pattern VALOR = Pattern.compile("NULL|CAST\\(convert_from\\(decode\\('([A-Za-z0-9+/=]*)','base64'\\),'UTF8'\\) AS ([a-z ]+)\\)");
	private static final List<TabelaBackupSql> TABELAS = TabelaBackupSql.todas();
	private static final String NOMES_TABELAS = String.join(", ", TABELAS.stream().map(TabelaBackupSql::nome).toList());

	private final JdbcTemplate jdbc;
	private final TransactionTemplate transacao;
	private final TransactionTemplate leitura;
	private final BackupProperties properties;
	private final UsuarioAtual usuarioAtual;
	private final UsuarioRepository usuarios;
    private final ReautenticacaoService reautenticacao;
	private final ChecksumService checksum;
	private final VersoesBackupService versoes;
	private final BackupAuditoriaService auditoria;
	private final Clock clock;
	private final Map<String, Pendente> pendentes = new ConcurrentHashMap<>();
	private final Map<String, List<String>> tiposPorTabela = new ConcurrentHashMap<>();

	public BackupSqlService(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
			BackupProperties properties, UsuarioAtual usuarioAtual, UsuarioRepository usuarios,
			ReautenticacaoService reautenticacao, ChecksumService checksum, VersoesBackupService versoes,
			BackupAuditoriaService auditoria, Clock clock) {
		this.jdbc = jdbc;
		this.transacao = new TransactionTemplate(transactionManager);
		this.leitura = new TransactionTemplate(transactionManager);
		this.leitura.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.leitura.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
		this.leitura.setReadOnly(true);
		this.leitura.setTimeout(Math.toIntExact(properties.getTimeout().toSeconds()));
		this.properties = properties;
		this.usuarioAtual = usuarioAtual;
		this.usuarios = usuarios;
        this.reautenticacao = reautenticacao;
		this.checksum = checksum;
		this.versoes = versoes;
		this.auditoria = auditoria;
		this.clock = clock;
	}

	public BackupArquivo exportar(String senhaAtual) {
		Usuario responsavel = autenticar(senhaAtual);
		Path diretorio = null;
		try {
			Files.createDirectories(properties.getDiretorioTemporario());
			diretorio = Files.createTempDirectory(properties.getDiretorioTemporario(), "apecan-sql-");
			LocalDateTime agora = LocalDateTime.now(clock);
			Path arquivo = diretorio.resolve("dados.sql");
			leitura.executeWithoutResult(status -> {
				long registros = quantidadeRegistros();
				try { escrever(arquivo, agora, registros); }
				catch (IOException exception) { throw new java.io.UncheckedIOException(exception); }
			});
			if (Files.size(arquivo) > properties.getTamanhoMaximo().toBytes())
				throw new OperacaoInvalidaException("BACKUP_MUITO_GRANDE", "O arquivo SQL excede o limite configurado.");
			String hash = checksum.sha256(arquivo);
			auditoria.registrarExportacao(ResultadoBackup.SUCESSO, versoes.versaoEsquemaAtual(), hash, null, responsavel);
			return new BackupArquivo(arquivo, "apecan-dados-" + agora.format(NOME) + ".sql", hash);
		}
		catch (RuntimeException | IOException exception) {
			ArquivosTemporarios.apagarRecursivamente(diretorio);
			throw exception instanceof RuntimeException runtime ? runtime
				: new OperacaoInvalidaException("FALHA_BACKUP_SQL", "Não foi possível gerar o arquivo SQL.");
		}
	}

	public void limpar(BackupArquivo arquivo) {
		if (arquivo != null) ArquivosTemporarios.apagarRecursivamente(arquivo.caminho().getParent());
	}

	public PreparacaoImportacaoSql analisar(MultipartFile upload) {
		limparExpirados();
		Long usuarioId = usuarioAtual.exigirId();
		pendentes.entrySet().removeIf(item -> {
			if (!item.getValue().usuarioId().equals(usuarioId)) return false;
			ArquivosTemporarios.apagarRecursivamente(item.getValue().arquivo().getParent());
			return true;
		});
		if (upload == null || upload.isEmpty())
			throw new OperacaoInvalidaException("ARQUIVO_SQL_OBRIGATORIO", "Selecione um arquivo SQL.");
		String nome = nomeSeguro(upload.getOriginalFilename());
		if (!nome.toLowerCase(java.util.Locale.ROOT).endsWith(".sql"))
			throw new OperacaoInvalidaException("FORMATO_BACKUP_INVALIDO", "Selecione um arquivo .sql gerado pelo APECAN.");
		if (upload.getSize() > properties.getTamanhoMaximo().toBytes())
			throw new OperacaoInvalidaException("BACKUP_MUITO_GRANDE", "O arquivo ultrapassa o limite configurado.");
		Path diretorio = null;
		try {
			Files.createDirectories(properties.getDiretorioTemporario());
			diretorio = Files.createTempDirectory(properties.getDiretorioTemporario(), "apecan-importacao-sql-");
			Path arquivo = diretorio.resolve("entrada.sql");
			try (var entrada = upload.getInputStream(); var saida = Files.newOutputStream(arquivo)) {
				byte[] buffer = new byte[8192];
				long total = 0;
				int lidos;
				while ((lidos = entrada.read(buffer)) >= 0) {
					total += lidos;
					if (total > properties.getTamanhoMaximo().toBytes())
						throw new OperacaoInvalidaException("BACKUP_MUITO_GRANDE", "O arquivo ultrapassa o limite configurado.");
					saida.write(buffer, 0, lidos);
				}
			}
			ResumoArquivoSql resumo = validar(arquivo, nome, Files.size(arquivo), false);
			String token = UUID.randomUUID().toString();
			Pendente pendente = new Pendente(usuarioId, arquivo, resumo,
				LocalDateTime.now(clock).plusMinutes(30));
			pendentes.put(token, pendente);
			return new PreparacaoImportacaoSql(token, resumo, resumoAtual());
		}
		catch (RuntimeException | IOException exception) {
			ArquivosTemporarios.apagarRecursivamente(diretorio);
			throw exception instanceof RuntimeException runtime ? runtime
				: new OperacaoInvalidaException("FALHA_UPLOAD_BACKUP", "Não foi possível receber o arquivo SQL.");
		}
	}

	public void importar(String token, String senhaAtual) {
		Pendente pendente = pendentes.get(token);
		if (pendente == null || pendente.expiraEm().isBefore(LocalDateTime.now(clock))
				|| !pendente.usuarioId().equals(usuarioAtual.exigirId())) {
			throw new OperacaoInvalidaException("IMPORTACAO_EXPIRADA", "A análise expirou. Selecione o arquivo novamente.");
		}
		Usuario responsavel = autenticar(senhaAtual);
		String hash = checksum.sha256(pendente.arquivo());
		try {
			transacao.executeWithoutResult(status -> {
				aplicar(pendente.arquivo());
				auditoria.registrarImportacaoSql(ResultadoBackup.SUCESSO, pendente.resumo().versaoEsquema(), hash,
					null, responsavel);
			});
		}
		finally {
			pendentes.remove(token);
			ArquivosTemporarios.apagarRecursivamente(pendente.arquivo().getParent());
		}
	}

	public PreparacaoImportacaoSql obterPreparacao(String token) {
		Pendente pendente = pendentes.get(token);
		if (pendente == null || pendente.expiraEm().isBefore(LocalDateTime.now(clock))
				|| !pendente.usuarioId().equals(usuarioAtual.exigirId()))
			throw new OperacaoInvalidaException("IMPORTACAO_EXPIRADA", "A análise expirou. Selecione o arquivo novamente.");
		return new PreparacaoImportacaoSql(token, pendente.resumo(), resumoAtual());
	}

	public void cancelar(String token) {
		Pendente pendente = pendentes.get(token);
		if (pendente != null && pendente.usuarioId().equals(usuarioAtual.exigirId())
				&& pendentes.remove(token, pendente))
			ArquivosTemporarios.apagarRecursivamente(pendente.arquivo().getParent());
	}

	private void escrever(Path arquivo, LocalDateTime agora, long registros) throws IOException {
		try (BufferedWriter out = new BufferedWriter(new java.io.OutputStreamWriter(new com.aclg.apecan.shared.io.SaidaLimitada(Files.newOutputStream(arquivo), properties.getTamanhoMaximo().toBytes()), StandardCharsets.UTF_8))) {
			out.write("-- APECAN-SQL-BACKUP:" + FORMATO); out.newLine();
			out.write("-- exportado-em:" + agora); out.newLine();
			out.write("-- esquema:" + versoes.versaoEsquemaAtual()); out.newLine();
			out.write("-- registros:" + registros); out.newLine();
			out.write("-- Contem dados pessoais. Armazene este arquivo em local protegido."); out.newLine();
			out.write("BEGIN;"); out.newLine();
			out.write("TRUNCATE TABLE " + NOMES_TABELAS + " RESTART IDENTITY;"); out.newLine();
			for (TabelaBackupSql tabela : TABELAS) {
				out.write("-- tabela:" + tabela.nome()); out.newLine();
				escreverTabela(out, tabela);
				out.write("-- fim:" + tabela.nome()); out.newLine();
			}
			for (TabelaBackupSql tabela : TABELAS) {
				out.write("UPDATE " + tabela.nome() + " SET importado_em = CURRENT_TIMESTAMP;"); out.newLine();
				if (tabela.colunaId() != null) {
					out.write("SELECT setval(pg_get_serial_sequence('" + tabela.nome() + "','" + tabela.colunaId()
						+ "'), COALESCE(MAX(" + tabela.colunaId() + "), 1), MAX(" + tabela.colunaId()
						+ ") IS NOT NULL) FROM " + tabela.nome() + ";"); out.newLine();
				}
			}
			out.write("COMMIT;"); out.newLine();
		}
	}

	private void escreverTabela(BufferedWriter out, TabelaBackupSql tabela) {
		String colunas = String.join(",", tabela.colunas());
		jdbc.query(conexao -> {
			var consulta = conexao.prepareStatement("SELECT " + colunas + " FROM " + tabela.nome() + ordem(tabela));
			consulta.setFetchSize(250);
			return consulta;
		}, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
			ResultSetMetaData meta = rs.getMetaData();
			List<String> valores = new ArrayList<>();
			for (int i = 1; i <= meta.getColumnCount(); i++) {
				String valor = rs.getString(i);
				valores.add(valor == null ? "NULL" : expressao(valor, tipo(meta.getColumnType(i))));
			}
			try {
				out.write("INSERT INTO " + tabela.nome() + " (" + colunas + ") VALUES ("
					+ String.join(",", valores) + ");");
				out.newLine();
			}
			catch (IOException exception) { throw new java.io.UncheckedIOException(exception); }
		});
	}

	private ResumoArquivoSql validar(Path arquivo, String nome, long tamanho, boolean aplicar) {
		try (BufferedReader in = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
			String formato = valorCabecalho(lerLinha(in), "-- APECAN-SQL-BACKUP:");
			if (!FORMATO.equals(formato)) throw invalido();
			LocalDateTime data = LocalDateTime.parse(valorCabecalho(lerLinha(in), "-- exportado-em:"));
			String esquema = valorCabecalho(lerLinha(in), "-- esquema:");
			if (!versoes.suporta(esquema)) throw new OperacaoInvalidaException("VERSAO_BACKUP_INCOMPATIVEL",
				"O arquivo foi criado por uma versão incompatível do APECAN.");
			long declarados = Long.parseLong(valorCabecalho(lerLinha(in), "-- registros:"));
			if (lerLinha(in) == null || !"BEGIN;".equals(lerLinha(in))) throw invalido();
			if (!("TRUNCATE TABLE " + NOMES_TABELAS + " RESTART IDENTITY;").equals(lerLinha(in))) throw invalido();
			long contados = 0;
			for (TabelaBackupSql tabela : TABELAS) {
				if (!("-- tabela:" + tabela.nome()).equals(lerLinha(in))) throw invalido();
				String linha;
				while ((linha = lerLinha(in)) != null && !linha.equals("-- fim:" + tabela.nome())) {
					LinhaInsert insert = interpretar(linha, tabela);
					if (aplicar) inserir(tabela, insert);
					contados++;
				}
				if (linha == null) throw invalido();
			}
			for (TabelaBackupSql tabela : TABELAS) {
				if (!("UPDATE " + tabela.nome() + " SET importado_em = CURRENT_TIMESTAMP;").equals(lerLinha(in))) throw invalido();
				if (tabela.colunaId() != null && !linhaSequencia(tabela).equals(lerLinha(in))) throw invalido();
			}
			if (!"COMMIT;".equals(lerLinha(in)) || lerLinha(in) != null || declarados != contados) throw invalido();
			return new ResumoArquivoSql(nome, tamanho, data, esquema, contados);
		}
		catch (OperacaoInvalidaException exception) { throw exception; }
		catch (Exception exception) { throw invalido(); }
	}

	private void aplicar(Path arquivo) {
		jdbc.execute("SELECT pg_advisory_xact_lock(92731101)");
		jdbc.execute("LOCK TABLE " + NOMES_TABELAS + " IN ACCESS EXCLUSIVE MODE");
		jdbc.execute("TRUNCATE TABLE " + NOMES_TABELAS + " RESTART IDENTITY");
		validar(arquivo, arquivo.getFileName().toString(), tamanho(arquivo), true);
		LocalDateTime importadoEm = LocalDateTime.now(clock);
		for (TabelaBackupSql tabela : TABELAS) {
			jdbc.update("UPDATE " + tabela.nome() + " SET importado_em = ?", importadoEm);
			if (tabela.colunaId() != null) jdbc.execute(linhaSequencia(tabela));
		}
	}

	private void inserir(TabelaBackupSql tabela, LinhaInsert linha) {
		String marcadores = String.join(",", linha.tipos().stream().map(tipo -> "CAST(? AS " + tipo + ")").toList());
		jdbc.update("INSERT INTO " + tabela.nome() + " (" + String.join(",", tabela.colunas()) + ") VALUES (" + marcadores + ")",
			linha.valores().toArray());
	}

	private LinhaInsert interpretar(String linha, TabelaBackupSql tabela) {
		Matcher insert = INSERT.matcher(linha);
		if (!insert.matches() || !tabela.nome().equals(insert.group(1))
				|| !String.join(",", tabela.colunas()).equals(insert.group(2))) throw invalido();
		List<String> expressoes = separar(insert.group(3));
		if (expressoes.size() != tabela.colunas().size()) throw invalido();
		List<Object> valores = new ArrayList<>();
		List<String> tipos = tipos(tabela);
		for (int i = 0; i < expressoes.size(); i++) {
			String expressao = expressoes.get(i);
			if ("NULL".equals(expressao)) { valores.add(null); continue; }
			Matcher valor = VALOR.matcher(expressao);
			if (!valor.matches() || !tipos.get(i).equals(valor.group(2))) throw invalido();
			valores.add(new String(Base64.getDecoder().decode(valor.group(1)), StandardCharsets.UTF_8));
		}
		return new LinhaInsert(valores, tipos);
	}

	private List<String> tipos(TabelaBackupSql tabela) {
		return tiposPorTabela.computeIfAbsent(tabela.nome(), ignorada -> jdbc.query(
			"SELECT " + String.join(",", tabela.colunas()) + " FROM " + tabela.nome() + " LIMIT 0", rs -> {
			ResultSetMetaData meta = rs.getMetaData();
			List<String> tipos = new ArrayList<>();
			for (int i = 1; i <= meta.getColumnCount(); i++) tipos.add(tipo(meta.getColumnType(i)));
			return tipos;
		}));
	}

	private List<String> separar(String texto) {
		List<String> itens = new ArrayList<>();
		int inicio = 0, nivel = 0;
		boolean aspas = false;
		for (int i = 0; i < texto.length(); i++) {
			char c = texto.charAt(i);
			if (c == '\'' && (i == 0 || texto.charAt(i - 1) != '\\')) aspas = !aspas;
			if (!aspas) {
				if (c == '(') nivel++;
				else if (c == ')') nivel--;
				else if (c == ',' && nivel == 0) { itens.add(texto.substring(inicio, i)); inicio = i + 1; }
			}
		}
		itens.add(texto.substring(inicio));
		return itens;
	}

	private ResumoDadosAtuais resumoAtual() {
		Long tamanho = jdbc.queryForObject("""
			SELECT COALESCE(SUM(pg_total_relation_size(c.oid)), 0)
			FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
			WHERE n.nspname = 'public' AND c.relname = ANY(string_to_array(?, ', '))
			""", Long.class, NOMES_TABELAS);
		LocalDateTime ultima = jdbc.queryForObject("""
			SELECT MAX(data) FROM (
			 SELECT MAX(criado_em) data FROM pacientes UNION ALL SELECT MAX(alterado_em) FROM historico_status_paciente
			 UNION ALL SELECT MAX(criado_em) FROM voluntarios UNION ALL SELECT MAX(criado_em) FROM categorias_equipamentos
			 UNION ALL SELECT MAX(criado_em) FROM equipamentos UNION ALL SELECT MAX(data_emprestimo::timestamp) FROM emprestimos_equipamentos
			 UNION ALL SELECT MAX(criado_em) FROM doacoes UNION ALL SELECT MAX(criado_em) FROM movimentacoes_financeiras
			 UNION ALL SELECT MAX(criado_em) FROM despesas
			) datas
			""", LocalDateTime.class);
		return new ResumoDadosAtuais(tamanho == null ? 0 : tamanho, quantidadeRegistros(), ultima);
	}

	private long quantidadeRegistros() {
		long total = 0;
		for (TabelaBackupSql tabela : TABELAS) total += jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela.nome(), Long.class);
		return total;
	}

	private Usuario autenticar(String senha) {
		Usuario usuario = usuarios.findById(usuarioAtual.exigirId()).orElseThrow(() ->
			new OperacaoInvalidaException("USUARIO_NAO_ENCONTRADO", "Usuário autenticado inválido."));
		reautenticacao.validar(usuario, senha);
		return usuario;
	}

	private String expressao(String valor, String tipo) {
		String base64 = Base64.getEncoder().encodeToString(valor.getBytes(StandardCharsets.UTF_8));
		return "CAST(convert_from(decode('" + base64 + "','base64'),'UTF8') AS " + tipo + ")";
	}

	private String tipo(int jdbcType) {
		return switch (jdbcType) {
			case Types.BIGINT -> "bigint";
			case Types.INTEGER -> "integer";
			case Types.NUMERIC, Types.DECIMAL -> "numeric";
			case Types.BOOLEAN, Types.BIT -> "boolean";
			case Types.DATE -> "date";
			case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "timestamp without time zone";
			case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR -> "character varying";
			default -> throw new OperacaoInvalidaException("TIPO_SQL_NAO_SUPORTADO", "O esquema contém um tipo não suportado pelo backup SQL.");
		};
	}

	private String ordem(TabelaBackupSql tabela) {
		return tabela.colunaId() == null ? " ORDER BY " + String.join(",", tabela.colunas().subList(0, Math.min(3, tabela.colunas().size())))
			: " ORDER BY " + tabela.colunaId();
	}

	private String linhaSequencia(TabelaBackupSql tabela) {
		return "SELECT setval(pg_get_serial_sequence('" + tabela.nome() + "','" + tabela.colunaId()
			+ "'), COALESCE(MAX(" + tabela.colunaId() + "), 1), MAX(" + tabela.colunaId()
			+ ") IS NOT NULL) FROM " + tabela.nome() + ";";
	}

	private String valorCabecalho(String linha, String prefixo) {
		if (linha == null || !linha.startsWith(prefixo)) throw invalido();
		return linha.substring(prefixo.length());
	}

	private String lerLinha(BufferedReader in) throws IOException {
		StringBuilder linha = new StringBuilder();
		int caractere;
		while ((caractere = in.read()) >= 0) {
			if (caractere == '\n') break;
			if (caractere != '\r') linha.append((char) caractere);
			if (linha.length() > 65_536) throw invalido();
		}
		return caractere < 0 && linha.isEmpty() ? null : linha.toString();
	}

	private String nomeSeguro(String nome) {
		if (nome == null || nome.isBlank()) return "backup.sql";
		return Path.of(nome).getFileName().toString();
	}

	private long tamanho(Path arquivo) {
		try { return Files.size(arquivo); }
		catch (IOException exception) { throw invalido(); }
	}

	private OperacaoInvalidaException invalido() {
		return new OperacaoInvalidaException("BACKUP_SQL_INVALIDO", "O arquivo não foi gerado pelo APECAN ou está corrompido.");
	}

	private void limparExpirados() {
		LocalDateTime agora = LocalDateTime.now(clock);
		pendentes.entrySet().removeIf(item -> {
			if (item.getValue().expiraEm().isAfter(agora)) return false;
			ArquivosTemporarios.apagarRecursivamente(item.getValue().arquivo().getParent());
			return true;
		});
	}

	@PreDestroy
	void encerrar() {
		pendentes.values().forEach(p -> ArquivosTemporarios.apagarRecursivamente(p.arquivo().getParent()));
		pendentes.clear();
	}

	private record LinhaInsert(List<Object> valores, List<String> tipos) {}
	private record Pendente(Long usuarioId, Path arquivo, ResumoArquivoSql resumo, LocalDateTime expiraEm) {}
}
