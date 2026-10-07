package com.aclg.apecan.regressao;

import com.aclg.apecan.auth.dto.AlterarSenhaForm;
import com.aclg.apecan.auth.security.UsuarioPrincipal;
import com.aclg.apecan.auth.service.AtivacaoUsuarioService;
import com.aclg.apecan.auth.service.CredencialService;
import com.aclg.apecan.doacao.dto.DoacaoForm;
import com.aclg.apecan.doacao.entity.TipoDoacao;
import com.aclg.apecan.doacao.service.DoacaoService;
import com.aclg.apecan.equipamento.dto.CategoriaEquipamentoForm;
import com.aclg.apecan.equipamento.entity.EstadoConservacao;
import com.aclg.apecan.equipamento.service.EquipamentoService;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.usuario.service.UsuarioService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regressões de segurança com dados fictícios. */
@SpringBootTest
@Transactional
class RevisaoSegurancaTests {
    @Autowired UsuarioService usuarios;
    @Autowired UsuarioRepository repository;
    @Autowired AtivacaoUsuarioService ativacao;
    @Autowired CredencialService credenciais;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.aclg.apecan.auth.service.EntregaRedefinicaoSenha entrega;
    @Autowired EquipamentoService equipamentos;
    @Autowired DoacaoService doacoes;
    private static final String SENHA = "Senha ficticia 2026";

    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    private void preparar() {
        NovoUsuarioForm f = new NovoUsuarioForm();
        f.setNome("Administrador Revisao"); f.setLogin("admin.revisao");
        f.setCpf("52998224725"); f.setEmail("revisao@example.invalid");
        f.setTelefone("14999999999");
        var criado = usuarios.cadastrarPrimeiroAdministrador(f);
        ativacao.ativar(token(criado.ativacao().linkLocal()), SENHA, SENHA);
        var principal = UsuarioPrincipal.de(repository.findByLogin("admin.revisao").orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test void trocarSenhaDeveRevogarLinkDeRecuperacaoAnterior() {
        preparar();
        org.mockito.Mockito.when(entrega.entregar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(new com.aclg.apecan.auth.service.RedefinicaoEmitida(null));
        credenciais.solicitar("revisao@example.invalid");
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(entrega).entregar(org.mockito.ArgumentMatchers.any(), captor.capture());
        String antigo = captor.getValue();
        AlterarSenhaForm f = new AlterarSenhaForm();
        f.setSenhaAtual(SENHA); f.setNovaSenha("Nova senha ficticia 2026");
        f.setConfirmacaoSenha("Nova senha ficticia 2026");
        credenciais.alterar(f);
        assertThatThrownBy(() -> credenciais.consultar(antigo))
            .isInstanceOf(OperacaoInvalidaException.class);
    }

    @Test void servicoDeveRejeitarDoacaoDeZeroEquipamentos() {
        preparar();
        CategoriaEquipamentoForm c = new CategoriaEquipamentoForm();
        c.setNome("Categoria Revisao"); c.setDescricao("Categoria ficticia");
        Long categoria = equipamentos.cadastrarCategoria(c);
        DoacaoForm f = new DoacaoForm();
        f.setTipo(TipoDoacao.EQUIPAMENTO); f.setDataDoacao(LocalDate.now());
        f.setFonteDoacao("Fonte ficticia"); f.setCategoriaId(categoria);
        f.setEstadoConservacao(EstadoConservacao.BOM); f.setQuantidadeEquipamentos(0);
        assertThatThrownBy(() -> doacoes.registrar(f))
            .isInstanceOfAny(ConstraintViolationException.class, OperacaoInvalidaException.class);
    }

    private static String token(String link) {
        return URI.create(link).getRawQuery().substring("token=".length());
    }
}
