package com.aclg.apecan.auth.controller;

import com.aclg.apecan.auth.dto.UsuarioNavegacaoDto;
import com.aclg.apecan.auth.security.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ContextoInterfaceControllerAdvice {

	@ModelAttribute("usuarioLogado")
	UsuarioNavegacaoDto usuarioLogado(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
			return null;
		}
		return new UsuarioNavegacaoDto(
			principal.getId(), principal.getNome(), principal.getUsername(), principal.getTipoPerfil());
	}

	@ModelAttribute("caminhoAtual")
	String caminhoAtual(HttpServletRequest request) {
		String caminho = request.getRequestURI().substring(request.getContextPath().length());
		return caminho.isBlank() ? "/" : caminho;
	}
}
