package com.aclg.apecan.backup.controller;

import com.aclg.apecan.backup.dto.ExportarBackupForm;
import com.aclg.apecan.backup.service.BackupArquivo;
import com.aclg.apecan.backup.service.BackupService;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Controller
@RequestMapping("/administracao/backups")
public class BackupAdminController {

	private final BackupService backupService;

	public BackupAdminController(BackupService backupService) {
		this.backupService = backupService;
	}

	@GetMapping
	String pagina(Model model) {
		if (!model.containsAttribute("exportarBackupForm")) {
			model.addAttribute("exportarBackupForm", new ExportarBackupForm());
		}
		return "backups/painel";
	}

	@PostMapping("/exportar")
	Object exportar(@Valid @ModelAttribute("exportarBackupForm") ExportarBackupForm formulario,
			BindingResult bindingResult, Model model) {
		if (formulario.getSenhaBackup() != null
				&& !formulario.getSenhaBackup().equals(formulario.getConfirmacaoSenhaBackup())) {
			bindingResult.rejectValue("confirmacaoSenhaBackup", "CONFIRMACAO_SENHA_INVALIDA",
				"A confirmação da senha do backup não confere.");
		}
		if (bindingResult.hasErrors()) {
			return "backups/painel";
		}
		BackupArquivo arquivo = null;
		try {
			arquivo = backupService.exportar(formulario.getSenhaAtual(), formulario.getSenhaBackup());
			BackupArquivo arquivoGerado = arquivo;
			long tamanho = Files.size(arquivoGerado.caminho());
			Resource corpo = recursoParaDownload(arquivoGerado, tamanho);
			return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.contentLength(tamanho)
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivoGerado.nomeArquivo() + "\"")
				.header("X-Content-Type-Options", "nosniff")
				.body(corpo);
		}
		catch (RegraNegocioException exception) {
			backupService.limpar(arquivo);
			bindingResult.reject(exception.getCodigo(), exception.getMessage());
			return "backups/painel";
		}
		catch (Exception exception) {
			backupService.limpar(arquivo);
			bindingResult.reject("FALHA_BACKUP", "Não foi possível gerar o backup.");
			return "backups/painel";
		}
	}

	private Resource recursoParaDownload(BackupArquivo arquivo, long tamanho) throws IOException {
		InputStream entrada = Files.newInputStream(arquivo.caminho());
		FilterInputStream entradaComLimpeza = new FilterInputStream(entrada) {
			private boolean fechada;

			@Override
			public void close() throws IOException {
				if (fechada) {
					return;
				}
				fechada = true;
				try {
					super.close();
				}
				finally {
					backupService.limpar(arquivo);
				}
			}
		};
		return new InputStreamResource(entradaComLimpeza, arquivo.nomeArquivo()) {
			@Override
			public long contentLength() {
				return tamanho;
			}

			@Override
			public String getFilename() {
				return arquivo.nomeArquivo();
			}
		};
	}
}
