package com.aclg.apecan.backup.service;

import com.aclg.apecan.backup.config.BackupProperties;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackupCriptografiaServiceTests {

	@TempDir
	Path temporario;

	@Test
	void deveEmpacotarAbrirEConfirmarIntegridade() throws Exception {
		BackupCriptografiaService service = service();
		Path trabalho = Files.createDirectory(temporario.resolve("trabalho"));
		Path dump = trabalho.resolve("database.dump");
		Files.writeString(dump, "conteudo-postgresql-de-teste");
		String checksum = new ChecksumService().sha256(dump);
		BackupManifest manifesto = manifesto(checksum);

		Path arquivo = service.empacotar(dump, manifesto, "senha-segura-de-backup".toCharArray(), trabalho);

		assertThat(new String(Files.readAllBytes(arquivo), StandardCharsets.ISO_8859_1))
			.doesNotContain("conteudo-postgresql-de-teste");
		try (BackupExtraido extraido = service.abrir(arquivo, "senha-segura-de-backup".toCharArray())) {
			assertThat(extraido.manifesto()).isEqualTo(manifesto);
			assertThat(Files.readString(extraido.dump())).isEqualTo("conteudo-postgresql-de-teste");
		}
	}

	@Test
	void deveRejeitarSenhaIncorretaEArquivoAdulterado() throws Exception {
		BackupCriptografiaService service = service();
		Path trabalho = Files.createDirectory(temporario.resolve("trabalho-invalido"));
		Path dump = trabalho.resolve("database.dump");
		Files.writeString(dump, "dump-de-teste");
		Path arquivo = service.empacotar(dump, manifesto(new ChecksumService().sha256(dump)),
			"senha-segura-de-backup".toCharArray(), trabalho);

		assertThatThrownBy(() -> service.abrir(arquivo, "senha-totalmente-errada".toCharArray()))
			.isInstanceOf(OperacaoInvalidaException.class);

		byte[] adulterado = Files.readAllBytes(arquivo);
		adulterado[adulterado.length - 1] ^= 1;
		Path alterado = temporario.resolve("alterado.apecan-backup");
		Files.write(alterado, adulterado);
		assertThatThrownBy(() -> service.abrir(alterado, "senha-segura-de-backup".toCharArray()))
			.isInstanceOf(OperacaoInvalidaException.class);
	}

	private BackupCriptografiaService service() {
		BackupProperties properties = new BackupProperties();
		properties.setDiretorioTemporario(temporario);
		properties.setTamanhoMaximo(DataSize.ofMegabytes(10));
		properties.setTamanhoDescompactadoMaximo(DataSize.ofMegabytes(20));
		properties.setIteracoesPbkdf2(100_000);
		return new BackupCriptografiaService(properties,
			JsonMapper.builder().findAndAddModules().build(), new ChecksumService());
	}

	private BackupManifest manifesto(String checksum) {
		return new BackupManifest(1, "teste", "9", "17", LocalDateTime.of(2026, 9, 12, 10, 0),
			"America/Sao_Paulo", Map.of("database.dump", checksum), List.of("BANCO_POSTGRESQL"));
	}
}
