package com.aclg.apecan.auth.controller;

import com.aclg.apecan.auth.dto.AlterarSenhaForm;
import com.aclg.apecan.auth.dto.RedefinirSenhaForm;
import com.aclg.apecan.auth.dto.SolicitarRedefinicaoForm;
import com.aclg.apecan.auth.service.CredencialService;
import com.aclg.apecan.auth.service.RedefinicaoEmitida;
import com.aclg.apecan.shared.exception.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CredencialController {

	private final CredencialService credencialService;

	public CredencialController(CredencialService credencialService) {
		this.credencialService = credencialService;
	}

	@GetMapping("/esqueci-senha")
	String solicitarForm(Model model) {
		if (!model.containsAttribute("solicitarRedefinicaoForm")) {
			model.addAttribute("solicitarRedefinicaoForm", new SolicitarRedefinicaoForm());
		}
		return "esqueci-senha";
	}

	@PostMapping("/esqueci-senha")
	String solicitar(@Valid @ModelAttribute SolicitarRedefinicaoForm solicitarRedefinicaoForm,
			BindingResult bindingResult, Model model) {
		if (bindingResult.hasErrors()) {
			return "esqueci-senha";
		}
		RedefinicaoEmitida emitida = credencialService.solicitar(solicitarRedefinicaoForm.getEmail());
		model.addAttribute("solicitacaoConcluida", true);
		model.addAttribute("linkLocal", emitida.linkLocal());
		solicitarRedefinicaoForm.setEmail("");
		return "esqueci-senha";
	}

	@GetMapping("/redefinir-senha")
	String redefinirForm(@RequestParam(required = false) String token, Model model) {
		RedefinirSenhaForm form = new RedefinirSenhaForm();
		form.setToken(token);
		model.addAttribute("redefinirSenhaForm", form);
		prepararToken(token, model);
		return "redefinir-senha";
	}

	@PostMapping("/redefinir-senha")
	String redefinir(@Valid @ModelAttribute RedefinirSenhaForm redefinirSenhaForm, BindingResult bindingResult,
			Model model) {
		if (bindingResult.hasErrors()) {
			prepararToken(redefinirSenhaForm.getToken(), model);
			return "redefinir-senha";
		}
		try {
			credencialService.redefinir(redefinirSenhaForm);
			return "redirect:/login?senhaRedefinida";
		}
		catch (RegraNegocioException exception) {
			model.addAttribute("erroRedefinicao", exception.getMessage());
			prepararToken(redefinirSenhaForm.getToken(), model);
			return "redefinir-senha";
		}
	}

	@GetMapping("/minha-conta/senha")
	String alterarForm(Model model) {
		if (!model.containsAttribute("alterarSenhaForm")) {
			model.addAttribute("alterarSenhaForm", new AlterarSenhaForm());
		}
		return "minha-conta/senha";
	}

	@PostMapping("/minha-conta/senha")
	String alterar(@Valid @ModelAttribute AlterarSenhaForm alterarSenhaForm, BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return "minha-conta/senha";
		}
		try {
			credencialService.alterar(alterarSenhaForm);
			redirectAttributes.addFlashAttribute("sucesso", "Senha alterada. Entre novamente.");
			return "redirect:/login";
		}
		catch (RegraNegocioException exception) {
			model.addAttribute("erroSenha", exception.getMessage());
			return "minha-conta/senha";
		}
	}

	private void prepararToken(String token, Model model) {
		try {
			model.addAttribute("tokenInfo", credencialService.consultar(token));
			model.addAttribute("tokenValido", true);
		}
		catch (RegraNegocioException exception) {
			model.addAttribute("tokenValido", false);
			model.addAttribute("erroRedefinicao", exception.getMessage());
		}
	}

}
