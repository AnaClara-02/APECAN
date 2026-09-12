package com.aclg.apecan.backup.service;

import com.aclg.apecan.backup.config.BackupProperties;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class BackupCriptografiaService {

	private static final byte[] ASSINATURA = "APECAN01".getBytes(StandardCharsets.US_ASCII);
	private static final int FORMATO = 1;
	private static final int SAL_TAMANHO = 16;
	private static final int NONCE_TAMANHO = 12;
	private static final int CHAVE_BITS = 256;
	private static final int TAG_BITS = 128;
	private static final int MANIFESTO_MAXIMO = 64 * 1024;
	private static final Set<String> ENTRADAS_PERMITIDAS = Set.of("manifest.json", "database.dump");

	private final BackupProperties properties;
	private final ObjectMapper objectMapper;
	private final ChecksumService checksumService;
	private final SecureRandom secureRandom = new SecureRandom();

	public BackupCriptografiaService(BackupProperties properties, ObjectMapper objectMapper,
			ChecksumService checksumService) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.checksumService = checksumService;
	}

	public Path empacotar(Path dump, BackupManifest manifesto, char[] senha, Path diretorioTrabalho) {
		Path zip = diretorioTrabalho.resolve("conteudo.zip");
		Path destino = diretorioTrabalho.resolve("backup.apecan-backup");
		try {
			criarZip(zip, dump, manifesto);
			criptografar(zip, destino, senha);
			return destino;
		}
		catch (IOException | GeneralSecurityException exception) {
			throw new IllegalStateException("Não foi possível proteger o arquivo de backup.", exception);
		}
		finally {
			ArquivosTemporarios.apagarSilenciosamente(zip);
		}
	}

	public BackupExtraido abrir(Path origem, char[] senha) {
		if (tamanho(origem) > properties.getTamanhoMaximo().toBytes()) {
			throw invalido("BACKUP_MUITO_GRANDE", "O arquivo ultrapassa o tamanho máximo permitido.");
		}
		Path diretorio = criarDiretorioTemporario("apecan-restauracao-");
		Path zip = diretorio.resolve("conteudo.zip");
		try {
			descriptografar(origem, zip, senha);
			BackupExtraido extraido = extrairZip(zip, diretorio);
			validar(extraido);
			return extraido;
		}
		catch (OperacaoInvalidaException exception) {
			ArquivosTemporarios.apagarRecursivamente(diretorio);
			throw exception;
		}
		catch (IOException | GeneralSecurityException exception) {
			ArquivosTemporarios.apagarRecursivamente(diretorio);
			throw invalido("BACKUP_INVALIDO", "Arquivo inválido, adulterado ou senha incorreta.");
		}
		finally {
			ArquivosTemporarios.apagarSilenciosamente(zip);
		}
	}

	public Path criarDiretorioTemporario(String prefixo) {
		try {
			Path raiz = properties.getDiretorioTemporario().toAbsolutePath().normalize();
			Files.createDirectories(raiz);
			return Files.createTempDirectory(raiz, prefixo);
		}
		catch (IOException exception) {
			throw new IllegalStateException("Não foi possível preparar a área temporária do backup.", exception);
		}
	}

	private void criarZip(Path zip, Path dump, BackupManifest manifesto) throws IOException {
		try (ZipOutputStream saida = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zip)))) {
			saida.putNextEntry(new ZipEntry("manifest.json"));
			saida.write(objectMapper.writeValueAsBytes(manifesto));
			saida.closeEntry();
			saida.putNextEntry(new ZipEntry("database.dump"));
			Files.copy(dump, saida);
			saida.closeEntry();
		}
	}

	private void criptografar(Path origem, Path destino, char[] senha)
			throws IOException, GeneralSecurityException {
		byte[] sal = aleatorio(SAL_TAMANHO);
		byte[] nonce = aleatorio(NONCE_TAMANHO);
		SecretKey chave = derivar(senha, sal, properties.getIteracoesPbkdf2());
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, nonce));
		try (DataOutputStream cabecalho = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(destino)))) {
			cabecalho.write(ASSINATURA);
			cabecalho.writeInt(FORMATO);
			cabecalho.writeInt(properties.getIteracoesPbkdf2());
			cabecalho.writeInt(sal.length);
			cabecalho.write(sal);
			cabecalho.writeInt(nonce.length);
			cabecalho.write(nonce);
			cabecalho.flush();
			try (CipherOutputStream cifrada = new CipherOutputStream(cabecalho, cipher)) {
				Files.copy(origem, cifrada);
			}
		}
	}

	private void descriptografar(Path origem, Path destino, char[] senha)
			throws IOException, GeneralSecurityException {
		try (DataInputStream entrada = new DataInputStream(new BufferedInputStream(Files.newInputStream(origem)))) {
			byte[] assinatura = entrada.readNBytes(ASSINATURA.length);
			if (!Arrays.equals(assinatura, ASSINATURA) || entrada.readInt() != FORMATO) {
				throw invalido("FORMATO_BACKUP_INVALIDO", "O arquivo não é um backup compatível do APECAN.");
			}
			int iteracoes = entrada.readInt();
			if (iteracoes < 100_000 || iteracoes > 5_000_000) {
				throw invalido("FORMATO_BACKUP_INVALIDO", "Os parâmetros do backup são inválidos.");
			}
			byte[] sal = lerCampo(entrada, SAL_TAMANHO);
			byte[] nonce = lerCampo(entrada, NONCE_TAMANHO);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, derivar(senha, sal, iteracoes), new GCMParameterSpec(TAG_BITS, nonce));
			try (CipherInputStream decifrada = new CipherInputStream(entrada, cipher);
					OutputStream saida = new BufferedOutputStream(Files.newOutputStream(destino))) {
				decifrada.transferTo(saida);
			}
		}
	}

	private BackupExtraido extrairZip(Path zip, Path diretorio) throws IOException {
		BackupManifest manifesto = null;
		Path dump = diretorio.resolve("database.dump");
		long total = 0;
		Set<String> encontradas = new HashSet<>();
		try (ZipInputStream entrada = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip)))) {
			ZipEntry item;
			while ((item = entrada.getNextEntry()) != null) {
				String nome = item.getName();
				if (item.isDirectory() || !ENTRADAS_PERMITIDAS.contains(nome) || !encontradas.add(nome)) {
					throw invalido("CONTEUDO_BACKUP_INVALIDO", "O conteúdo do backup não é permitido.");
				}
				if ("manifest.json".equals(nome)) {
					byte[] json = lerLimitado(entrada, MANIFESTO_MAXIMO);
					total += json.length;
					manifesto = objectMapper.readValue(json, BackupManifest.class);
				}
				else {
					total += copiarLimitado(entrada, dump, properties.getTamanhoDescompactadoMaximo().toBytes() - total);
				}
				entrada.closeEntry();
			}
		}
		if (manifesto == null || !Files.isRegularFile(dump) || !encontradas.equals(ENTRADAS_PERMITIDAS)) {
			throw invalido("CONTEUDO_BACKUP_INCOMPLETO", "O backup está incompleto.");
		}
		return new BackupExtraido(diretorio, dump, manifesto);
	}

	private void validar(BackupExtraido extraido) {
		BackupManifest manifesto = extraido.manifesto();
		if (manifesto.formato() != FORMATO || manifesto.checksums() == null || manifesto.componentes() == null
				|| !manifesto.componentes().contains("BANCO_POSTGRESQL")) {
			throw invalido("MANIFESTO_BACKUP_INVALIDO", "O manifesto do backup é incompatível.");
		}
		String esperado = manifesto.checksums().get("database.dump");
		String encontrado = checksumService.sha256(extraido.dump());
		if (esperado == null || !MessageDigestIsEqual.comparar(esperado, encontrado)) {
			throw invalido("CHECKSUM_BACKUP_INVALIDO", "A integridade do backup não pôde ser confirmada.");
		}
	}

	private SecretKey derivar(char[] senha, byte[] sal, int iteracoes) throws GeneralSecurityException {
		PBEKeySpec especificacao = new PBEKeySpec(senha, sal, iteracoes, CHAVE_BITS);
		try {
			byte[] chave = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacao).getEncoded();
			try {
				return new SecretKeySpec(chave, "AES");
			}
			finally {
				Arrays.fill(chave, (byte) 0);
			}
		}
		finally {
			especificacao.clearPassword();
		}
	}

	private byte[] lerCampo(DataInputStream entrada, int tamanhoEsperado) throws IOException {
		int tamanho = entrada.readInt();
		if (tamanho != tamanhoEsperado) {
			throw invalido("FORMATO_BACKUP_INVALIDO", "Os parâmetros do backup são inválidos.");
		}
		byte[] campo = entrada.readNBytes(tamanho);
		if (campo.length != tamanho) {
			throw invalido("FORMATO_BACKUP_INVALIDO", "O arquivo de backup está truncado.");
		}
		return campo;
	}

	private byte[] lerLimitado(InputStream entrada, int limite) throws IOException {
		byte[] conteudo = entrada.readNBytes(limite + 1);
		if (conteudo.length > limite) {
			throw invalido("BACKUP_DESCOMPACTADO_MUITO_GRANDE", "O conteúdo do backup ultrapassa o limite permitido.");
		}
		return conteudo;
	}

	private long copiarLimitado(InputStream entrada, Path destino, long limite) throws IOException {
		if (limite <= 0) {
			throw invalido("BACKUP_DESCOMPACTADO_MUITO_GRANDE", "O conteúdo do backup ultrapassa o limite permitido.");
		}
		long total = 0;
		byte[] buffer = new byte[8192];
		try (OutputStream saida = new BufferedOutputStream(Files.newOutputStream(destino))) {
			int lidos;
			while ((lidos = entrada.read(buffer)) >= 0) {
				total += lidos;
				if (total > limite) {
					throw invalido("BACKUP_DESCOMPACTADO_MUITO_GRANDE", "O conteúdo do backup ultrapassa o limite permitido.");
				}
				saida.write(buffer, 0, lidos);
			}
		}
		return total;
	}

	private long tamanho(Path arquivo) {
		try {
			return Files.size(arquivo);
		}
		catch (IOException exception) {
			throw invalido("BACKUP_ILEGIVEL", "Não foi possível ler o arquivo de backup.");
		}
	}

	private byte[] aleatorio(int tamanho) {
		byte[] bytes = new byte[tamanho];
		secureRandom.nextBytes(bytes);
		return bytes;
	}

	private OperacaoInvalidaException invalido(String codigo, String mensagem) {
		return new OperacaoInvalidaException(codigo, mensagem);
	}

	private static final class MessageDigestIsEqual {
		private MessageDigestIsEqual() {
		}

		static boolean comparar(String primeiro, String segundo) {
			return java.security.MessageDigest.isEqual(
				primeiro.getBytes(StandardCharsets.US_ASCII), segundo.getBytes(StandardCharsets.US_ASCII));
		}
	}
}
