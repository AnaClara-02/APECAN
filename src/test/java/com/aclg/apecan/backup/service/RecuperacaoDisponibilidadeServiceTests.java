package com.aclg.apecan.backup.service;

import com.aclg.apecan.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecuperacaoDisponibilidadeServiceTests {

	@Test
	void devePermitirInstalacaoVaziaSomenteNoServidor() {
		UsuarioRepository repository = mock(UsuarioRepository.class);
		when(repository.count()).thenReturn(0L);
		RecuperacaoDisponibilidadeService service = new RecuperacaoDisponibilidadeService(repository, false);

		MockHttpServletRequest local = new MockHttpServletRequest();
		local.setRemoteAddr("127.0.0.1");
		MockHttpServletRequest remoto = new MockHttpServletRequest();
		remoto.setRemoteAddr("192.168.1.20");

		assertThat(service.permitida(local)).isTrue();
		assertThat(service.permitida(remoto)).isFalse();
	}

	@Test
	void deveExigirModoDeRecuperacaoQuandoExistiremUsuarios() {
		UsuarioRepository repository = mock(UsuarioRepository.class);
		when(repository.count()).thenReturn(2L);
		MockHttpServletRequest local = new MockHttpServletRequest();
		local.setRemoteAddr("::1");

		assertThat(new RecuperacaoDisponibilidadeService(repository, false).permitida(local)).isFalse();
		assertThat(new RecuperacaoDisponibilidadeService(repository, true).permitida(local)).isTrue();
	}
}
