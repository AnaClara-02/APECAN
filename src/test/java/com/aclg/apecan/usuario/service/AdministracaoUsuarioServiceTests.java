package com.aclg.apecan.usuario.service;

import com.aclg.apecan.auth.security.UsuarioPrincipal;
import com.aclg.apecan.auth.service.AtivacaoUsuarioService;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.usuario.dto.AlterarAcessoForm;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.dto.UsuarioCriadoResultado;
import com.aclg.apecan.usuario.entity.TipoEventoAdministracaoUsuario;
import com.aclg.apecan.usuario.entity.TipoPerfil;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.HistoricoAdministracaoUsuarioRepository;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AdministracaoUsuarioServiceTests {

    private static final String SENHA_ADMIN = "Senha admin segura 2026";
    private static final String SENHA_SEGUNDO = "Senha segundo admin 2026";

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AtivacaoUsuarioService ativacaoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HistoricoAdministracaoUsuarioRepository historicoRepository;

    @Autowired
    private SessionRegistry sessionRegistry;

    @AfterEach
    void limparSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveTransferirAdministracaoEProtegerUltimoAdministrador() {
        Usuario adminInicial = criarEAtivarPrimeiroAdministrador();
        autenticar(adminInicial);

        UsuarioCriadoResultado segundoResultado = usuarioService.cadastrar(formularioSegundo());
        String tokenSegundo = extrairToken(segundoResultado.ativacao().linkLocal());
        ativacaoService.ativar(tokenSegundo, SENHA_SEGUNDO, SENHA_SEGUNDO);
        Usuario segundo = usuarioRepository.findByLogin("segundo.admin").orElseThrow();

        AlterarAcessoForm confirmacaoInicial = confirmacao(SENHA_ADMIN, "Sucessao administrativa planejada.");
        usuarioService.promover(segundo.getId(), confirmacaoInicial);
        assertThat(segundo.getTipoPerfil()).isEqualTo(TipoPerfil.ADMINISTRADOR);

        UsuarioPrincipal principalInicial = UsuarioPrincipal.de(adminInicial);
        sessionRegistry.registerNewSession("sessao-admin-inicial", principalInicial);
        usuarioService.rebaixar(
            adminInicial.getId(),
            confirmacao(SENHA_ADMIN, "Transferencia concluida para o sucessor.")
        );

        SessionInformation sessao = sessionRegistry.getSessionInformation("sessao-admin-inicial");
        // A transação do teste ainda está aberta; a revogação só ocorre após commit.
        assertThat(sessao.isExpired()).isFalse();
        assertThat(adminInicial.getTipoPerfil()).isEqualTo(TipoPerfil.USUARIO);

        autenticar(segundo);
        assertThatThrownBy(() -> usuarioService.rebaixar(
            segundo.getId(),
            confirmacao(SENHA_SEGUNDO, "Tentativa de remover o ultimo administrador.")
        ))
            .isInstanceOf(OperacaoInvalidaException.class)
            .hasMessageContaining("ultimo administrador");

        assertThat(historicoRepository.findAll()).extracting("tipoEvento")
            .contains(
                TipoEventoAdministracaoUsuario.PROMOCAO_ADMINISTRADOR,
                TipoEventoAdministracaoUsuario.REBAIXAMENTO_USUARIO
            );
    }

    private Usuario criarEAtivarPrimeiroAdministrador() {
        NovoUsuarioForm formulario = new NovoUsuarioForm();
        formulario.setNome("Administrador Inicial");
        formulario.setLogin("admin.inicial");
        formulario.setCpf("52998224725");
        formulario.setEmail("admin.inicial@apecan.org.br");
        formulario.setTelefone("14999999999");
        UsuarioCriadoResultado resultado =
            usuarioService.cadastrarPrimeiroAdministrador(formulario);
        ativacaoService.ativar(extrairToken(resultado.ativacao().linkLocal()), SENHA_ADMIN, SENHA_ADMIN);
        return usuarioRepository.findByLogin("admin.inicial").orElseThrow();
    }

    private NovoUsuarioForm formularioSegundo() {
        NovoUsuarioForm formulario = new NovoUsuarioForm();
        formulario.setNome("Segundo Administrador");
        formulario.setLogin("segundo.admin");
        formulario.setCpf("11144477735");
        formulario.setEmail("segundo.admin@apecan.org.br");
        formulario.setTelefone("14988888888");
        return formulario;
    }

    private AlterarAcessoForm confirmacao(String senha, String justificativa) {
        AlterarAcessoForm formulario = new AlterarAcessoForm();
        formulario.setSenhaAtual(senha);
        formulario.setJustificativa(justificativa);
        return formulario;
    }

    private void autenticar(Usuario usuario) {
        UsuarioPrincipal principal = UsuarioPrincipal.de(usuario);
        UsernamePasswordAuthenticationToken autenticacao =
            UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
            );
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }

    private String extrairToken(String link) {
        return URI.create(link).getRawQuery().substring("token=".length());
    }
}
