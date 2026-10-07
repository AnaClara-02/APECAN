package com.aclg.apecan.regressao;

import com.aclg.apecan.auth.security.UsuarioPrincipal;
import com.aclg.apecan.auth.service.*;
import com.aclg.apecan.usuario.dto.*;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.usuario.service.UsuarioService;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:tokens_ciclo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@Transactional
class CicloTokensSegurancaTests {
    @Autowired UsuarioService usuarios;
    @Autowired UsuarioRepository repository;
    @Autowired AtivacaoUsuarioService ativacao;
    @Autowired CredencialService credenciais;
    @Autowired PasswordEncoder encoder;
    @Autowired WebApplicationContext context;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoBean EntregaRedefinicaoSenha entrega;
    static final String SENHA="Senha ficticia auditoria 2026";
    static final String NOVA="Outra senha ficticia 2026";
    Long alvo;
    String convite;
    String emailAntigo=java.util.UUID.randomUUID()+"@example.invalid";
    MockMvc mvc;

    @BeforeEach void configurarMvc(){mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
    @AfterEach void limpar(){SecurityContextHolder.clearContext();}
    NovoUsuarioForm formulario(String login,String cpf,String email){
        NovoUsuarioForm form=new NovoUsuarioForm(); form.setNome("Pessoa Auditoria");
        form.setLogin(login); form.setCpf(cpf); form.setEmail(email); form.setTelefone("14999999999"); return form;
    }
    String token(String link){return URI.create(link).getRawQuery().substring(6);}
    void autenticarAdministrador(){
        var principal=UsuarioPrincipal.de(repository.findByLogin("admin.ciclo").orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
    }
    void preparar(boolean ativarAlvo){
        var admin=usuarios.cadastrarPrimeiroAdministrador(formulario("admin.ciclo","52998224725","admin.ciclo@example.invalid"));
        ativacao.ativar(token(admin.ativacao().linkLocal()),SENHA,SENHA);
        autenticarAdministrador();
        var usuario=usuarios.cadastrar(formulario("alvo.ciclo","11144477735",emailAntigo));
        alvo=repository.findByLogin("alvo.ciclo").orElseThrow().getId();
        convite=token(usuario.ativacao().linkLocal());
        if(ativarAlvo) ativacao.ativar(convite,SENHA,SENHA);
    }
    String emitirReset(String email){
        AtomicReference<String> capturado=new AtomicReference<>();
        when(entrega.entregar(any(),anyString())).thenAnswer(invocacao->{capturado.set(invocacao.getArgument(1));return new RedefinicaoEmitida(null);});
        credenciais.solicitar(email);
        assertThat(capturado.get()).isNotBlank(); return capturado.get();
    }
    void mudarEmail(){var form=usuarios.formularioEdicao(alvo); form.setEmail("novo@example.invalid"); usuarios.atualizar(alvo,form);}
    void desativarEReativar(){
        AlterarAcessoForm form=new AlterarAcessoForm(); form.setSenhaAtual(SENHA); form.setJustificativa("Ciclo de auditoria");
        usuarios.desativar(alvo,form);usuarios.reativar(alvo,form);
    }
    void rejeitarResetAnonimo(String token) throws Exception {
        String hashAntes=repository.findById(alvo).orElseThrow().getSenhaHash();
        SecurityContextHolder.clearContext();
        mvc.perform(get("/redefinir-senha").param("token",token)).andExpect(model().attribute("tokenValido",false));
        mvc.perform(post("/redefinir-senha").with(csrf()).param("token",token).param("senha",NOVA).param("confirmacaoSenha",NOVA))
            .andExpect(status().isOk()).andExpect(model().attribute("tokenValido",false));
        assertThat(repository.findById(alvo).orElseThrow().getSenhaHash()).isEqualTo(hashAntes);
    }
    void rejeitarConviteAnonimo(String token) throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/ativar-conta").param("token",token)).andExpect(model().attribute("tokenValido",false));
        mvc.perform(post("/ativar-conta").with(csrf()).param("token",token).param("senha",NOVA).param("confirmacaoSenha",NOVA))
            .andExpect(status().isOk()).andExpect(model().attribute("tokenValido",false));
        assertThat(repository.findById(alvo).orElseThrow().estaAtivado()).isFalse();
    }
    @Test void trocarEmailRevogaResetAnterior() throws Exception {
        preparar(true);String antigo=emitirReset(emailAntigo);mudarEmail();rejeitarResetAnonimo(antigo);
    }
    @Test void desativarEReativarNaoRestauraResetAnterior() throws Exception {
        preparar(true);String antigo=emitirReset(emailAntigo);desativarEReativar();rejeitarResetAnonimo(antigo);
    }
    @Test void trocarEmailRevogaConviteAnterior() throws Exception {
        preparar(false);mudarEmail();rejeitarConviteAnonimo(convite);
    }
    @Test void desativarEReativarNaoRestauraConviteAnterior() throws Exception {
        preparar(false);desativarEReativar();rejeitarConviteAnonimo(convite);
    }
    void reativarContaLegada(){
        // Simula conta desativada pela versão anterior, que preservava os tokens.
        repository.findByIdForUpdate(alvo).orElseThrow().desativar(java.time.LocalDateTime.now());
        repository.flush();
        var form=new AlterarAcessoForm();form.setSenhaAtual(SENHA);form.setJustificativa("Reativacao de conta legada");
        usuarios.reativar(alvo,form);
    }
    @Test void reativarContaLegadaRevogaResetPendente() throws Exception {
        preparar(true);String antigo=emitirReset(emailAntigo);reativarContaLegada();rejeitarResetAnonimo(antigo);
    }
    @Test void reativarContaLegadaRevogaConvitePendente() throws Exception {
        preparar(false);reativarContaLegada();rejeitarConviteAnonimo(convite);
    }
    @Test void resetNovoUsaEmailAtualizadoEFuncionaUmaVez() throws Exception {
        preparar(true);emitirReset(emailAntigo);mudarEmail();String novo=emitirReset("novo@example.invalid");
        verify(entrega,atLeastOnce()).entregar(argThat(usuario->usuario.getEmail().equals("novo@example.invalid")),eq(novo));
        SecurityContextHolder.clearContext();
        mvc.perform(post("/redefinir-senha").with(csrf()).param("token",novo).param("senha",NOVA).param("confirmacaoSenha",NOVA))
            .andExpect(redirectedUrl("/login?senhaRedefinida"));
        assertThat(encoder.matches(NOVA,repository.findById(alvo).orElseThrow().getSenhaHash())).isTrue();
        rejeitarResetAnonimo(novo);
    }
    @Test void conviteNovoFuncionaUmaVezAposTrocaDeEmail() throws Exception {
        preparar(false);mudarEmail();String novo=token(usuarios.reemitirAtivacao(alvo).linkLocal());
        SecurityContextHolder.clearContext();
        mvc.perform(post("/ativar-conta").with(csrf()).param("token",novo).param("senha",NOVA).param("confirmacaoSenha",NOVA))
            .andExpect(redirectedUrl("/login?ativada"));
        assertThat(repository.findById(alvo).orElseThrow().estaAtivado()).isTrue();
        assertThatThrownBy(()->ativacao.ativar(novo,SENHA,SENHA)).isInstanceOf(OperacaoInvalidaException.class);
        assertThat(encoder.matches(NOVA,repository.findById(alvo).orElseThrow().getSenhaHash())).isTrue();
    }
    @Test void editarSemMudarEmailPreservaConvite(){
        preparar(false);var form=usuarios.formularioEdicao(alvo);
        // Normalização de caixa não é uma troca de identidade.
        form.setEmail(emailAntigo.toUpperCase(java.util.Locale.ROOT));usuarios.atualizar(alvo,form);
        assertThat(ativacao.consultar(convite)).isNotNull();
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void rollbackDaEdicaoTambemDesfazRevogacao(){
        var transacao=new TransactionTemplate(transactionManager);
        try {
            transacao.executeWithoutResult(status->preparar(false));
            assertThatThrownBy(()->transacao.executeWithoutResult(status->{
                mudarEmail();throw new IllegalStateException("rollback proposital");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(repository.findById(alvo).orElseThrow().getEmail()).isEqualTo(emailAntigo);
            assertThat(ativacao.consultar(convite)).isNotNull();
        } finally {
            // Banco H2 exclusivo desta classe, somente dados fictícios.
            jdbc.update("DELETE FROM historico_administracao_usuarios");
            jdbc.update("DELETE FROM tokens_credencial");
            jdbc.update("DELETE FROM usuarios WHERE login='alvo.ciclo'");
            jdbc.update("DELETE FROM usuarios WHERE login='admin.ciclo'");
        }
    }
}
