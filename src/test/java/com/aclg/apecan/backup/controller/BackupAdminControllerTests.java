package com.aclg.apecan.backup.controller;

import com.aclg.apecan.backup.service.BackupArquivo;
import com.aclg.apecan.backup.service.BackupSqlService;
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
		BackupSqlService backupService = mock(BackupSqlService.class);
		Path arquivo = diretorioTemporario.resolve("backup.sql");
		byte[] conteudo = "-- APECAN-SQL-BACKUP:1".getBytes(StandardCharsets.UTF_8);
		Files.write(arquivo, conteudo);
		BackupArquivo backup = new BackupArquivo(arquivo, "apecan-dados-teste.sql", "checksum");
		when(backupService.exportar("senha-atual"))
			.thenReturn(backup);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BackupAdminController(backupService)).build();

		mockMvc.perform(post("/administracao/backups/exportacao")
				.param("senhaAtual", "senha-atual"))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(header().string("Content-Disposition",
				"attachment; filename=\"apecan-dados-teste.sql\""))
			.andExpect(content().bytes(conteudo));

		verify(backupService).limpar(backup);
	}
}
