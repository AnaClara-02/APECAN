package com.aclg.apecan.regressao;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest class CoberturaRotasTests {
 @Autowired WebApplicationContext context; MockMvc mvc;
 @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 @Test void backupPermiteUsuarioComumMasExigeDadosValidos() throws Exception {
  mvc.perform(get("/administracao/backups").with(user("auditoria").roles("USUARIO"))).andExpect(status().isOk());
  mvc.perform(get("/administracao/backups/exportacao").with(user("auditoria").roles("USUARIO"))).andExpect(status().isOk());
  mvc.perform(post("/administracao/backups/exportacao").with(user("auditoria").roles("USUARIO")).with(csrf()))
      .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("exportarBackupSqlForm", "senhaAtual"));
  mvc.perform(get("/administracao/backups/importacao").with(user("auditoria").roles("USUARIO"))).andExpect(status().isOk());
  mvc.perform(post("/administracao/backups/importacao/analisar").with(user("auditoria").roles("USUARIO")).with(csrf()))
      .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("importarBackupSqlForm", "arquivo"));
  mvc.perform(post("/administracao/backups/importacao/confirmar").param("token", "inexistente")
      .with(user("auditoria").roles("USUARIO")).with(csrf()))
      .andExpect(status().isFound()).andExpect(redirectedUrl("/administracao/backups/importacao"));
  mvc.perform(post("/administracao/backups/importacao/cancelar").param("token", "inexistente")
      .with(user("auditoria").roles("USUARIO")).with(csrf()))
      .andExpect(status().isFound()).andExpect(redirectedUrl("/administracao/backups/importacao"));
 }
 @Test void demaisRotasAdministrativasNegamUsuarioComum() throws Exception {
  mvc.perform(get("/usuarios").with(user("auditoria").roles("USUARIO"))).andExpect(status().isForbidden());
  mvc.perform(get("/usuarios/novo").with(user("auditoria").roles("USUARIO"))).andExpect(status().isForbidden());
  mvc.perform(post("/usuarios").with(user("auditoria").roles("USUARIO")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(get("/usuarios/999").with(user("auditoria").roles("USUARIO"))).andExpect(status().isForbidden());
  mvc.perform(get("/usuarios/999/editar").with(user("auditoria").roles("USUARIO"))).andExpect(status().isForbidden());
  mvc.perform(post("/usuarios/999/editar").with(user("auditoria").roles("USUARIO")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/usuarios/999/reenviar-ativacao").with(user("auditoria").roles("USUARIO")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/usuarios/999/promover").with(user("auditoria").roles("USUARIO")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/usuarios/999/rebaixar").with(user("auditoria").roles("USUARIO")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/usuarios/999/desativar").with(user("auditoria").roles("USUARIO")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/usuarios/999/reativar").with(user("auditoria").roles("USUARIO")).with(csrf())).andExpect(status().isForbidden());
 }
 @Test void todasAsRotasProtegidasNegamAnonimo() throws Exception {
  mvc.perform(get("/minha-conta/senha")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/minha-conta/senha").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/inicio")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/administracao/backups")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/administracao/backups/exportacao")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/administracao/backups/exportacao").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/administracao/backups/importacao")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/administracao/backups/importacao/analisar").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/administracao/backups/importacao/confirmar").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/administracao/backups/importacao/cancelar").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/doacoes")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/doacoes/nova")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/doacoes").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/emprestimos")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/emprestimos/novo")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/emprestimos").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/emprestimos/999")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/emprestimos/999/devolucao").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/equipamentos")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/equipamentos/novo")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/equipamentos").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/equipamentos/999")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/equipamentos/999/editar")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/equipamentos/999/editar").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/equipamentos/999/status").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/equipamentos/categorias").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/equipamentos/categorias/999/excluir").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/financeiro")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/financeiro/nova")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/financeiro").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/financeiro/despesas")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/financeiro/despesas/nova")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/financeiro/despesas").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/pacientes")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/pacientes/novo")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/pacientes").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/pacientes/999")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/pacientes/999/editar")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/pacientes/999/editar").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/pacientes/999/status").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/relatorios/pacientes/exportar")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/relatorios")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/usuarios")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/usuarios/novo")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/usuarios").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/usuarios/999")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/usuarios/999/editar")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/usuarios/999/editar").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/usuarios/999/reenviar-ativacao").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/usuarios/999/promover").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/usuarios/999/rebaixar").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/usuarios/999/desativar").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/usuarios/999/reativar").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/voluntarios")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/voluntarios/novo")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/voluntarios").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/voluntarios/999")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/voluntarios/999/editar")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/voluntarios/999/editar").with(csrf())).andExpect(status().is3xxRedirection());
  mvc.perform(post("/voluntarios/999/status").with(csrf())).andExpect(status().is3xxRedirection());
 }
}
