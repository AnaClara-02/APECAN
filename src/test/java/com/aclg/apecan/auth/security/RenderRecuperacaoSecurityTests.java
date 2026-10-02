package com.aclg.apecan.auth.security;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:render_security;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.flyway.enabled=false","apecan.mail.transporte=smtp",
    "management.endpoint.health.probes.enabled=true"
})
@ActiveProfiles("render")
class RenderRecuperacaoSecurityTests {
    // O validador de configuracao e testado separadamente; esta classe usa banco H2 isolado.
    @MockitoBean RenderSegurancaValidator validator;
    @Autowired WebApplicationContext context;
    @Test void bloqueiaRecuperacaoMesmoEmLoopbackComCabecalhosForjados() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/recuperacao").header("X-Forwarded-For","127.0.0.1")
            .with(request -> {request.setRemoteAddr("127.0.0.1"); return request;})).andExpect(status().isForbidden());
        mvc.perform(post("/recuperacao").with(csrf()).with(user("admin").roles("ADMINISTRADOR")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk())
            .andExpect(jsonPath("$.components").doesNotExist());
    }
}
