package com.aclg.apecan.auth.controller;

import com.aclg.apecan.auth.security.UsuarioPrincipal;
import com.aclg.apecan.usuario.entity.TipoPerfil;
import com.aclg.apecan.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

class ContextoInterfaceControllerAdviceTests {

	private final ContextoInterfaceControllerAdvice advice = new ContextoInterfaceControllerAdvice();

	@Test
	void deveExporUsuarioAutenticadoParaNavegacao() {
		Usuario usuario = new Usuario(
			"Ana Administradora", "ana.admin", "52998224725", "ana@apecan.org.br",
			"/images/usuario-padrao.svg", "14999999999", TipoPerfil.ADMINISTRADOR, null);
		UsuarioPrincipal principal = UsuarioPrincipal.de(usuario);
		var authentication = UsernamePasswordAuthenticationToken.authenticated(
			principal, principal.getPassword(), principal.getAuthorities());

		var contexto = advice.usuarioLogado(authentication);

		assertThat(contexto.nome()).isEqualTo("Ana Administradora");
		assertThat(contexto.login()).isEqualTo("ana.admin");
		assertThat(contexto.administrador()).isTrue();
	}

	@Test
	void deveOmitirUsuarioEmPaginaPublicaEExporCaminhoAtual() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/pacientes/novo");

		assertThat(advice.usuarioLogado(null)).isNull();
		assertThat(advice.caminhoAtual(request)).isEqualTo("/pacientes/novo");
	}
}
