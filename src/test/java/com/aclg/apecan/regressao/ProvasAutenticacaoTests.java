package com.aclg.apecan.regressao;
import com.aclg.apecan.auth.dto.AlterarSenhaForm;
import com.aclg.apecan.auth.security.UsuarioPrincipal;
import com.aclg.apecan.auth.service.*;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.usuario.service.UsuarioService;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.net.URI;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @Transactional
class ProvasAutenticacaoTests {
 @Autowired UsuarioService usuarios;
 @Autowired UsuarioRepository repository;
 @Autowired AtivacaoUsuarioService ativacao;
 @Autowired CredencialService credenciais;
 @Autowired WebApplicationContext context;
 static final String SENHA="Senha ficticia auditoria 2026";
 @AfterEach void limpar(){SecurityContextHolder.clearContext();}
 void preparar(String email){
  NovoUsuarioForm f=new NovoUsuarioForm(); f.setNome("Administrador Auditoria");
  f.setLogin("admin.auditoria"); f.setCpf("52998224725"); f.setEmail(email); f.setTelefone("14999999999");
  var criado=usuarios.cadastrarPrimeiroAdministrador(f);
  ativacao.ativar(URI.create(criado.ativacao().linkLocal()).getRawQuery().substring(6),SENHA,SENHA);
  var p=UsuarioPrincipal.de(repository.findByLogin("admin.auditoria").orElseThrow());
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,p.getAuthorities()));
 }
 @Test void anonimoNuncaRecebeTokenNoModoLocal() throws Exception {
  preparar("reset-publico@example.invalid"); SecurityContextHolder.clearContext();
  var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  var response=mvc.perform(post("/esqueci-senha").with(csrf()).param("email","reset-publico@example.invalid"))
   .andExpect(status().isOk()).andExpect(model().attributeDoesNotExist("linkLocal")).andReturn();
  assertThat(response.getResponse().getContentAsString()).doesNotContain("token=", "abrir redefinição");
  assertThat(new EntregaRedefinicaoSenhaLocal().entregar(null,"segredo-ficticio").linkLocal()).isNull();
 }
 @Test void falhasDeReautenticacaoBloqueiamInclusiveSenhaCorreta(){
  preparar("reauth@example.invalid");
  AlterarSenhaForm f=new AlterarSenhaForm(); f.setSenhaAtual("Senha incorreta 2026");
  f.setNovaSenha("Senha posterior auditoria 2026"); f.setConfirmacaoSenha("Senha posterior auditoria 2026");
  for(int i=0;i<8;i++) assertThatThrownBy(()->credenciais.alterar(f))
   .isInstanceOf(OperacaoInvalidaException.class).hasMessageContaining("senha atual");
  f.setSenhaAtual(SENHA); assertThatThrownBy(()->credenciais.alterar(f)).isInstanceOf(OperacaoInvalidaException.class);
 }
}
