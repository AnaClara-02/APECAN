package com.aclg.apecan.auth.service;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.dto.UsuarioCriadoResultado;
import com.aclg.apecan.usuario.entity.TokenCredencial;
import com.aclg.apecan.usuario.entity.Usuario;
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
class AtivacaoUsuarioServiceTests {

    private static final String SENHA = "Senha definitiva 2026";

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AtivacaoUsuarioService ativacaoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenCredencialRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void deveCriarAdministradorSemSenhaEAtivarComTokenDeUsoUnico() {
        UsuarioCriadoResultado resultado =
            usuarioService.cadastrarPrimeiroAdministrador(formularioAdministrador());
        String tokenSubstituido = extrairToken(resultado.ativacao().linkLocal());

        Usuario pendente = usuarioRepository.findByLogin("administrador")
            .orElseThrow();

        assertThat(pendente.getSenhaHash()).isNull();
        assertThat(pendente.isPrimeiroAcessoPendente()).isTrue();

        String tokenOriginal = extrairToken(
            ativacaoService.emitir(pendente).linkLocal()
        );
        TokenCredencial tokenPersistido = tokenRepository.findAll().stream()
            .filter(token -> !token.foiUtilizado())
            .findFirst()
            .orElseThrow();
        assertThat(tokenPersistido.getTokenHash())
            .hasSize(64)
            .doesNotContain(tokenOriginal);

        assertThatThrownBy(() ->
            ativacaoService.ativar(tokenSubstituido, SENHA, SENHA))
            .isInstanceOf(OperacaoInvalidaException.class)
            .hasMessageContaining("invalido");

        ativacaoService.ativar(tokenOriginal, SENHA, SENHA);
        usuarioRepository.flush();

        assertThat(pendente.estaAtivado()).isTrue();
        assertThat(pendente.getSenhaHash()).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches(SENHA, pendente.getSenhaHash())).isTrue();
        assertThat(tokenPersistido.foiUtilizado()).isTrue();

        assertThatThrownBy(() -> ativacaoService.ativar(tokenOriginal, SENHA, SENHA))
            .isInstanceOf(OperacaoInvalidaException.class)
            .hasMessageContaining("invalido");
    }

    @Test
    void naoDevePermitirSegundoBootstrapNemSenhaDivergente() {
        UsuarioCriadoResultado resultado =
            usuarioService.cadastrarPrimeiroAdministrador(formularioAdministrador());
        String tokenOriginal = extrairToken(resultado.ativacao().linkLocal());

        assertThatThrownBy(() ->
            usuarioService.cadastrarPrimeiroAdministrador(formularioAdministrador()))
            .isInstanceOf(OperacaoInvalidaException.class)
            .hasMessageContaining("ja foi concluida");

        assertThatThrownBy(() ->
            ativacaoService.ativar(tokenOriginal, SENHA, "Outra senha definitiva"))
            .isInstanceOf(OperacaoInvalidaException.class)
            .hasMessageContaining("nao corresponde");
    }

    private NovoUsuarioForm formularioAdministrador() {
        NovoUsuarioForm formulario = new NovoUsuarioForm();
        formulario.setNome("Administrador Inicial");
        formulario.setLogin("Administrador");
        formulario.setCpf("529.982.247-25");
        formulario.setEmail("ADMIN@APECAN.ORG.BR");
        formulario.setTelefone("(14) 99999-9999");
        return formulario;
    }

    private String extrairToken(String link) {
        String consulta = URI.create(link).getRawQuery();
        return consulta.substring("token=".length());
    }
}
