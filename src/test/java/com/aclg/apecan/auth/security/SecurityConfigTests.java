package com.aclg.apecan.auth.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityConfigTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepararMockMvc() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(applicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void paginaProtegidaDeveRedirecionarUsuarioAnonimoParaLogin() throws Exception {
        mockMvc.perform(get("/pagina-protegida"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void apiProtegidaDeveRetornarProblemDetailParaUsuarioAnonimo() throws Exception {
        mockMvc.perform(get("/api/recurso-protegido"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
            .andExpect(jsonPath("$.erroId").isNotEmpty());
    }

    @Test
    void usuarioComumNaoDeveAcessarAdministracao() throws Exception {
        mockMvc.perform(get("/usuarios").with(user("comum").roles("USUARIO")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/usuarios").with(user("comum").roles("USUARIO")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
	void postSemCsrfDeveSerRejeitado() throws Exception {
        mockMvc.perform(post("/operacao").with(user("comum").roles("USUARIO")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/operacao")
                .with(user("comum").roles("USUARIO"))
                .with(csrf()))
            .andExpect(status().isNotFound());
	}

	@Test
	void usuarioComumDeveAcessarModuloFuncional() throws Exception {
		mockMvc.perform(get("/pacientes").with(user("comum").roles("USUARIO")))
			.andExpect(status().isOk());
	}

    @Test
    void healthERecursosEstaticosDevemSerPublicosEConterCabecalhos() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/css/error.css"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().exists("Content-Security-Policy"))
            .andExpect(header().string(
                "Referrer-Policy",
                "no-referrer"
            ))
            .andExpect(header().string(
                "Permissions-Policy",
                "camera=(), microphone=(), geolocation=()"
            ));
    }

    @Test
    void deveGerarHashBcryptComIdentificadorDoAlgoritmo() {
        String hash = passwordEncoder.encode("Senha forte de teste");

        assertThat(hash).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches("Senha forte de teste", hash)).isTrue();
        assertThat(passwordEncoder.matches("Senha incorreta", hash)).isFalse();
    }
}
