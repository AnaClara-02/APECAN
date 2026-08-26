package com.aclg.apecan.doacao.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@Transactional
class DoacaoControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext applicationContext;

	@BeforeEach
	void preparar() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).apply(springSecurity()).build();
	}

	@Test
	@WithMockUser(roles = "USUARIO")
	void deveRenderizarGruposCondicionaisEMascaraMonetaria() throws Exception {
		mockMvc.perform(get("/doacoes/nova"))
			.andExpect(status().isOk())
			.andExpect(view().name("doacoes/formulario"))
			.andExpect(content().string(containsString("data-grupo-doacao=\"MONETARIA\"")))
			.andExpect(content().string(containsString("data-grupo-doacao=\"EQUIPAMENTO\"")))
			.andExpect(content().string(containsString("data-grupo-doacao=\"OUTRO_BEM\"")))
			.andExpect(content().string(containsString("data-moeda-brl")))
			.andExpect(content().string(containsString("placeholder=\"R$ 0,00\"")));
	}

}
