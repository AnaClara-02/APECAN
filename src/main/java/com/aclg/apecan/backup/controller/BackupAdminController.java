package com.aclg.apecan.backup.controller;

import com.aclg.apecan.backup.dto.ConfirmarImportacaoSqlForm;
import com.aclg.apecan.backup.dto.ExportarBackupSqlForm;
import com.aclg.apecan.backup.dto.ImportarBackupSqlForm;
import com.aclg.apecan.backup.service.BackupArquivo;
import com.aclg.apecan.backup.service.BackupSqlService;
import com.aclg.apecan.backup.service.PreparacaoImportacaoSql;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Controller
@RequestMapping("/administracao/backups")
public class BackupAdminController {

	private final BackupSqlService backups;

	public BackupAdminController(BackupSqlService backups) { this.backups = backups; }

	@GetMapping({"", "/exportacao"})
	String exportacao(Model model) {
		if (!model.containsAttribute("exportarBackupSqlForm"))
			model.addAttribute("exportarBackupSqlForm", new ExportarBackupSqlForm());
		return "backups/exportacao";
	}

	@PostMapping("/exportacao")
	Object exportar(@Valid @ModelAttribute("exportarBackupSqlForm") ExportarBackupSqlForm formulario,
			BindingResult erros) {
		if (erros.hasErrors()) return "backups/exportacao";
		BackupArquivo arquivo = null;
		try {
			arquivo = backups.exportar(formulario.getSenhaAtual());
			BackupArquivo gerado = arquivo;
			long tamanho = Files.size(gerado.caminho());
			Resource corpo = recursoParaDownload(gerado, tamanho);
			return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/sql"))
				.contentLength(tamanho).cacheControl(CacheControl.noStore())
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + gerado.nomeArquivo() + "\"")
				.header("X-Content-Type-Options", "nosniff").body(corpo);
		}
		catch (RegraNegocioException exception) {
			backups.limpar(arquivo);
			erros.reject(exception.getCodigo(), exception.getMessage());
			return "backups/exportacao";
		}
		catch (Exception exception) {
			backups.limpar(arquivo);
			erros.reject("FALHA_BACKUP_SQL", "Não foi possível gerar o arquivo SQL.");
			return "backups/exportacao";
		}
	}

	@GetMapping("/importacao")
	String importacao(Model model) {
		if (!model.containsAttribute("importarBackupSqlForm"))
			model.addAttribute("importarBackupSqlForm", new ImportarBackupSqlForm());
		return "backups/importacao";
	}

	@PostMapping("/importacao/analisar")
	String analisar(@ModelAttribute("importarBackupSqlForm") ImportarBackupSqlForm formulario,
			BindingResult erros, Model model) {
		MultipartFile arquivo = formulario.getArquivo();
		if (arquivo == null || arquivo.isEmpty())
			erros.rejectValue("arquivo", "ARQUIVO_SQL_OBRIGATORIO", "Selecione um arquivo SQL.");
		if (erros.hasErrors()) return "backups/importacao";
		try {
			PreparacaoImportacaoSql preparacao = backups.analisar(arquivo);
			ConfirmarImportacaoSqlForm confirmacao = new ConfirmarImportacaoSqlForm();
			confirmacao.setToken(preparacao.token());
			model.addAttribute("preparacao", preparacao);
			model.addAttribute("confirmarImportacaoSqlForm", confirmacao);
			return "backups/confirmar-importacao";
		}
		catch (RegraNegocioException exception) {
			erros.reject(exception.getCodigo(), exception.getMessage());
			return "backups/importacao";
		}
	}

	@PostMapping("/importacao/confirmar")
	String confirmar(@Valid @ModelAttribute("confirmarImportacaoSqlForm") ConfirmarImportacaoSqlForm formulario,
			BindingResult erros, Model model, RedirectAttributes redirect) {
		try {
			if (erros.hasErrors()) {
				model.addAttribute("preparacao", backups.obterPreparacao(formulario.getToken()));
				return "backups/confirmar-importacao";
			}
			backups.importar(formulario.getToken(), formulario.getSenhaAtual());
			redirect.addFlashAttribute("sucesso", "Dados importados com sucesso. As contas de acesso atuais foram preservadas.");
			return "redirect:/administracao/backups/importacao";
		}
		catch (RegraNegocioException exception) {
			erros.reject(exception.getCodigo(), exception.getMessage());
			try { model.addAttribute("preparacao", backups.obterPreparacao(formulario.getToken())); }
			catch (RegraNegocioException expirada) { return "redirect:/administracao/backups/importacao"; }
			return "backups/confirmar-importacao";
		}
	}

	@PostMapping("/importacao/cancelar")
	String cancelar(String token, RedirectAttributes redirect) {
		backups.cancelar(token);
		redirect.addFlashAttribute("sucesso", "Importação cancelada. Os dados atuais foram mantidos.");
		return "redirect:/administracao/backups/importacao";
	}

	private Resource recursoParaDownload(BackupArquivo arquivo, long tamanho) throws IOException {
		InputStream entrada = Files.newInputStream(arquivo.caminho());
		FilterInputStream comLimpeza = new FilterInputStream(entrada) {
			private boolean fechada;
			@Override public void close() throws IOException {
				if (fechada) return;
				fechada = true;
				try { super.close(); } finally { backups.limpar(arquivo); }
			}
		};
		return new InputStreamResource(comLimpeza, arquivo.nomeArquivo()) {
			@Override public long contentLength() { return tamanho; }
			@Override public String getFilename() { return arquivo.nomeArquivo(); }
		};
	}
}
