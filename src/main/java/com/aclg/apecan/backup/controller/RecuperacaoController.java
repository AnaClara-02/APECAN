package com.aclg.apecan.backup.controller;

import com.aclg.apecan.backup.dto.RestaurarBackupForm;
import com.aclg.apecan.backup.service.RecuperacaoDisponibilidadeService;
import com.aclg.apecan.backup.service.RecuperacaoService;
import com.aclg.apecan.backup.service.ResultadoRestauracao;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/recuperacao")
public class RecuperacaoController {

	private final RecuperacaoDisponibilidadeService disponibilidade;
	private final RecuperacaoService recuperacaoService;

	public RecuperacaoController(RecuperacaoDisponibilidadeService disponibilidade,
			RecuperacaoService recuperacaoService) {
		this.disponibilidade = disponibilidade;
		this.recuperacaoService = recuperacaoService;
	}

	@GetMapping
	String pagina(HttpServletRequest request, Model model) {
		exigirDisponibilidade(request);
		if (!model.containsAttribute("restaurarBackupForm")) {
			model.addAttribute("restaurarBackupForm", new RestaurarBackupForm());
		}
		model.addAttribute("modoRecuperacao", disponibilidade.modoRecuperacao());
		return "backups/recuperacao";
	}

	@PostMapping
	String restaurar(HttpServletRequest request,
			@Valid @ModelAttribute("restaurarBackupForm") RestaurarBackupForm formulario,
			BindingResult bindingResult, Model model) {
		exigirDisponibilidade(request);
		if (formulario.getArquivo() == null || formulario.getArquivo().isEmpty()) {
			bindingResult.rejectValue("arquivo", "ARQUIVO_BACKUP_OBRIGATORIO", "Selecione um arquivo de backup.");
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("modoRecuperacao", disponibilidade.modoRecuperacao());
			return "backups/recuperacao";
		}
		try {
			ResultadoRestauracao resultado = recuperacaoService.restaurar(formulario.getArquivo(),
				formulario.getSenhaBackup());
			model.addAttribute("resultado", resultado);
			return "backups/recuperacao-concluida";
		}
		catch (RegraNegocioException exception) {
			bindingResult.reject(exception.getCodigo(), exception.getMessage());
			model.addAttribute("modoRecuperacao", disponibilidade.modoRecuperacao());
			return "backups/recuperacao";
		}
	}

	private void exigirDisponibilidade(HttpServletRequest request) {
		if (!disponibilidade.permitida(request)) {
			throw new OperacaoInvalidaException("RECUPERACAO_INDISPONIVEL",
				"A recuperação só pode ser realizada localmente no servidor.");
		}
	}
}
