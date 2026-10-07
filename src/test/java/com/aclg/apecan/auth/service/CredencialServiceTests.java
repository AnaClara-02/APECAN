package com.aclg.apecan.auth.service;

import com.aclg.apecan.auth.dto.RedefinirSenhaForm;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.dto.UsuarioCriadoResultado;
import com.aclg.apecan.usuario.entity.FinalidadeTokenCredencial;
import com.aclg.apecan.usuario.repository.TokenCredencialRepository;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CredencialServiceTests {

	private static final String SENHA_ANTIGA = "Senha original 2026";

	private static final String SENHA_NOVA = "Senha renovada 2026";

	@Autowired
	UsuarioService usuarioService;

	@Autowired
	AtivacaoUsuarioService ativacaoService;

	@Autowired
	CredencialService credencialService;

	@Autowired
	UsuarioRepository usuarioRepository;

	@Autowired
	TokenCredencialRepository tokenRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    EntregaRedefinicaoSenha entrega;

	@Test
	void deveResponderGenericamenteEUsarTokenDeRedefinicaoUmaUnicaVez() {
		UsuarioCriadoResultado criado = usuarioService.cadastrarPrimeiroAdministrador(formulario());
		ativacaoService.ativar(extrairToken(criado.ativacao().linkLocal()), SENHA_ANTIGA, SENHA_ANTIGA);

		assertThat(credencialService.solicitar("inexistente@example.invalid").linkLocal()).isNull();
        org.mockito.Mockito.when(entrega.entregar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(new RedefinicaoEmitida(null));
        assertThat(credencialService.solicitar(" ADMIN.RESET@APECAN.ORG.BR ").linkLocal()).isNull();
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(entrega).entregar(org.mockito.ArgumentMatchers.any(), captor.capture());
        String token = captor.getValue();

		assertThat(tokenRepository.findAll())
			.anyMatch(persistido -> persistido.getFinalidade() == FinalidadeTokenCredencial.REDEFINICAO_SENHA
					&& !persistido.getTokenHash().contains(token));
		assertThat(credencialService.consultar(token).nomeUsuario()).isEqualTo("Administrador Reset");

		RedefinirSenhaForm form = new RedefinirSenhaForm();
		form.setToken(token);
		form.setSenha(SENHA_NOVA);
		form.setConfirmacaoSenha(SENHA_NOVA);
		credencialService.redefinir(form);

		String hash = usuarioRepository.findByLogin("admin.reset").orElseThrow().getSenhaHash();
		assertThat(passwordEncoder.matches(SENHA_NOVA, hash)).isTrue();
		assertThat(passwordEncoder.matches(SENHA_ANTIGA, hash)).isFalse();
		assertThatThrownBy(() -> credencialService.redefinir(form)).isInstanceOf(OperacaoInvalidaException.class)
			.hasMessageContaining("invalido");
	}

	private NovoUsuarioForm formulario() {
		NovoUsuarioForm form = new NovoUsuarioForm();
		form.setNome("Administrador Reset");
		form.setLogin("admin.reset");
		form.setCpf("52998224725");
		form.setEmail("admin.reset@apecan.org.br");
		form.setTelefone("14999999999");
		return form;
	}

	private String extrairToken(String link) {
		return URI.create(link).getRawQuery().substring("token=".length());
	}

}
