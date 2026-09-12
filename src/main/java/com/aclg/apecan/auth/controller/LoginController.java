package com.aclg.apecan.auth.controller;

import com.aclg.apecan.backup.service.RecuperacaoDisponibilidadeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

	private final RecuperacaoDisponibilidadeService recuperacaoDisponibilidade;

	public LoginController(RecuperacaoDisponibilidadeService recuperacaoDisponibilidade) {
		this.recuperacaoDisponibilidade = recuperacaoDisponibilidade;
	}

    @GetMapping("/login")
    String login(Authentication authentication, HttpServletRequest request, Model model) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/inicio";
        }
		model.addAttribute("recuperacaoDisponivel", recuperacaoDisponibilidade.permitida(request));
        return "login";
    }
}
