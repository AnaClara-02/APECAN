package com.aclg.apecan.backup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties("apecan.backup")
public class BackupProperties {

	private String pgDumpPath = "pg_dump";
	private String pgRestorePath = "pg_restore";
	private Path diretorioTemporario = Path.of(System.getProperty("java.io.tmpdir"), "apecan-backup");
	private DataSize tamanhoMaximo = DataSize.ofGigabytes(2);
	private DataSize tamanhoDescompactadoMaximo = DataSize.ofGigabytes(10);
	private Duration timeout = Duration.ofMinutes(30);
	private int iteracoesPbkdf2 = 600_000;
	private String sslMode;
	private String sslRootCert;
	private String usuario;
	private String senha;
	public String getSslMode() { return sslMode; }
	public void setSslMode(String value) { sslMode = value; }
	public String getSslRootCert() { return sslRootCert; }
	public void setSslRootCert(String value) { sslRootCert = value; }
	public String getUsuario() { return usuario; }
	public void setUsuario(String value) { usuario = value; }
	public String getSenha() { return senha; }
	public void setSenha(String value) { senha = value; }

	public String getPgDumpPath() { return pgDumpPath; }
	public void setPgDumpPath(String pgDumpPath) { this.pgDumpPath = pgDumpPath; }
	public String getPgRestorePath() { return pgRestorePath; }
	public void setPgRestorePath(String pgRestorePath) { this.pgRestorePath = pgRestorePath; }
	public Path getDiretorioTemporario() { return diretorioTemporario; }
	public void setDiretorioTemporario(Path diretorioTemporario) { this.diretorioTemporario = diretorioTemporario; }
	public DataSize getTamanhoMaximo() { return tamanhoMaximo; }
	public void setTamanhoMaximo(DataSize tamanhoMaximo) { this.tamanhoMaximo = tamanhoMaximo; }
	public DataSize getTamanhoDescompactadoMaximo() { return tamanhoDescompactadoMaximo; }
	public void setTamanhoDescompactadoMaximo(DataSize tamanhoDescompactadoMaximo) { this.tamanhoDescompactadoMaximo = tamanhoDescompactadoMaximo; }
	public Duration getTimeout() { return timeout; }
	public void setTimeout(Duration timeout) { this.timeout = timeout; }
	public int getIteracoesPbkdf2() { return iteracoesPbkdf2; }
	public void setIteracoesPbkdf2(int iteracoesPbkdf2) { this.iteracoesPbkdf2 = iteracoesPbkdf2; }
}
