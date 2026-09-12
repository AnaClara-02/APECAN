package com.aclg.apecan.backup.controller;

import com.aclg.apecan.backup.service.BackupArquivo;
import com.aclg.apecan.backup.service.BackupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BackupAdminControllerTests {

	@TempDir
	Path diretorioTemporario;

	@Test
	void deveTransmitirArquivoGeradoELimparTemporario() throws Exception {
		BackupService backupService = mock(BackupService.class);
		Path arquivo = diretorioTemporario.resolve("backup.apecan-backup");
		byte[] conteudo = "backup-criptografado".getBytes(StandardCharsets.UTF_8);
		Files.write(arquivo, conteudo);
		BackupArquivo backup = new BackupArquivo(arquivo, "apecan-backup-teste.apecan-backup", "checksum");
		when(backupService.exportar("senha-atual", "senha-de-backup-segura"))
			.thenReturn(backup);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BackupAdminController(backupService)).build();

		mockMvc.perform(post("/administracao/backups/exportar")
				.param("senhaAtual", "senha-atual")
				.param("senhaBackup", "senha-de-backup-segura")
				.param("confirmacaoSenhaBackup", "senha-de-backup-segura"))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(header().string("Content-Disposition",
				"attachment; filename=\"apecan-backup-teste.apecan-backup\""))
			.andExpect(content().bytes(conteudo));

		verify(backupService).limpar(backup);
	}
}
