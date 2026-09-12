package com.aclg.apecan.backup.service;

import com.aclg.apecan.backup.config.BackupProperties;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class PostgreSqlBackupClient {

	// Usa ProcessBuilder com argumentos separados para impedir interpretação pelo shell.
	private final BackupProperties properties;
	private final String url;
	private final String usuario;
	private final String senha;

	public PostgreSqlBackupClient(BackupProperties properties,
			@Value("${spring.datasource.url}") String url,
			@Value("${spring.datasource.username}") String usuario,
			@Value("${spring.datasource.password}") String senha) {
		this.properties = properties;
		this.url = url;
		this.usuario = usuario;
		this.senha = senha;
	}

	public void exportar(Path destino) {
		List<String> comando = comandoBase(properties.getPgDumpPath());
		comando.add("--format=custom");
		comando.add("--no-owner");
		comando.add("--no-privileges");
		comando.add("--file=" + destino.toAbsolutePath());
		executar(comando, properties.getTimeout(), "FALHA_PG_DUMP");
		if (!Files.isRegularFile(destino)) {
			throw new OperacaoInvalidaException("FALHA_PG_DUMP", "O PostgreSQL não gerou o arquivo de backup.");
		}
	}

	public void restaurar(Path dump) {
		List<String> comando = comandoBase(properties.getPgRestorePath());
		comando.add("--clean");
		comando.add("--if-exists");
		comando.add("--no-owner");
		comando.add("--no-privileges");
		comando.add("--single-transaction");
		comando.add("--exit-on-error");
		comando.add(dump.toAbsolutePath().toString());
		executar(comando, properties.getTimeout(), "FALHA_PG_RESTORE");
	}

	private List<String> comandoBase(String executavel) {
		ConexaoPostgreSql conexao = conexao();
		List<String> comando = new ArrayList<>();
		comando.add(executavel);
		comando.add("--host=" + conexao.host());
		comando.add("--port=" + conexao.porta());
		comando.add("--username=" + conexao.usuario());
		comando.add("--dbname=" + conexao.banco());
		comando.add("--no-password");
		return comando;
	}

	private void executar(List<String> comando, Duration timeout, String codigo) {
		ConexaoPostgreSql conexao = conexao();
		Path erro = null;
		try {
			erro = Files.createTempFile(properties.getDiretorioTemporario(), "postgres-", ".log");
			ProcessBuilder builder = new ProcessBuilder(comando);
			builder.environment().put("PGPASSWORD", conexao.senha());
			builder.redirectError(erro.toFile());
			builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
			Process processo = builder.start();
			boolean terminou = processo.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!terminou) {
				processo.destroyForcibly();
				throw new OperacaoInvalidaException(codigo, "A operação com o PostgreSQL excedeu o tempo permitido.");
			}
			if (processo.exitValue() != 0) {
				throw new OperacaoInvalidaException(codigo,
					"O PostgreSQL não concluiu a operação. Verifique as ferramentas e as credenciais configuradas.");
			}
		}
		catch (IOException exception) {
			throw new OperacaoInvalidaException("FERRAMENTA_POSTGRESQL_INDISPONIVEL",
				"Não foi possível executar pg_dump/pg_restore. Verifique o caminho configurado.");
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new OperacaoInvalidaException(codigo, "A operação de backup foi interrompida.");
		}
		finally {
			ArquivosTemporarios.apagarSilenciosamente(erro);
		}
	}

	private ConexaoPostgreSql conexao() {
		return ConexaoPostgreSql.de(url, usuario, senha);
	}

	private static final class ConexaoPostgreSql {
		private final String host;
		private final int porta;
		private final String banco;
		private final String usuario;
		private final String senha;

		private ConexaoPostgreSql(String host, int porta, String banco, String usuario, String senha) {
			this.host = host;
			this.porta = porta;
			this.banco = banco;
			this.usuario = usuario;
			this.senha = senha;
		}

		static ConexaoPostgreSql de(String jdbcUrl, String usuario, String senha) {
			try {
				if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
					throw new IllegalArgumentException();
				}
				URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
				String banco = uri.getPath();
				if (uri.getHost() == null || banco == null || banco.length() <= 1) {
					throw new IllegalArgumentException();
				}
				return new ConexaoPostgreSql(uri.getHost(), uri.getPort() < 0 ? 5432 : uri.getPort(),
					banco.substring(1), usuario, senha);
			}
			catch (RuntimeException exception) {
				throw new IllegalStateException("A URL JDBC do PostgreSQL não é compatível com o módulo de backup.");
			}
		}

		String host() { return host; }
		int porta() { return porta; }
		String banco() { return banco; }
		String usuario() { return usuario; }
		String senha() { return senha; }
	}
}
