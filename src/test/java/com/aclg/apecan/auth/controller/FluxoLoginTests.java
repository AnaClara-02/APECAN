package com.aclg.apecan.auth.controller;

import com.aclg.apecan.auth.service.AtivacaoUsuarioService;
import com.aclg.apecan.paciente.repository.PacienteRepository;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.dto.UsuarioCriadoResultado;
import com.aclg.apecan.usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FluxoLoginTests {

	private static final String SENHA = "Senha de acesso 2026";

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext applicationContext;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private AtivacaoUsuarioService ativacaoService;

	@Autowired
	private PacienteRepository pacienteRepository;

	@BeforeEach
	void prepararMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).apply(springSecurity()).build();
	}

	@Test
	void contaPendenteNaoDeveEntrarEContaAtivadaDeveEntrar() throws Exception {
		UsuarioCriadoResultado resultado = usuarioService.cadastrarPrimeiroAdministrador(formulario());

		mockMvc.perform(post("/login").param("login", "admin.web").param("senha", SENHA).with(csrf()))
			.andExpect(redirectedUrl("/login?erro"))
			.andExpect(unauthenticated());

		String token = URI.create(resultado.ativacao().linkLocal()).getRawQuery().substring("token=".length());
		ativacaoService.ativar(token, SENHA, SENHA);

		MockHttpSession sessao = (MockHttpSession) mockMvc
			.perform(post("/login").param("login", "ADMIN.WEB").param("senha", SENHA).with(csrf()))
			.andExpect(redirectedUrl("/inicio"))
			.andExpect(authenticated().withUsername("admin.web"))
			.andReturn()
			.getRequest()
			.getSession(false);

		mockMvc.perform(post("/pacientes").session(sessao).with(csrf())
				.param("nome", "Paciente 123")
				.param("cpf", "11144477735")
				.param("dataNascimento", "2000-01-01")
				.param("telefone", "14988888888")
				.param("endereco", "Rua das Flores, 10")
				.param("localTratamento", "Hospital 9 de Julho"))
			.andExpect(status().isOk())
			.andExpect(view().name("pacientes/novo"))
			.andExpect(model().attributeHasFieldErrors("novoPacienteForm", "nome"))
			.andExpect(content().string(containsString("Use somente letras, espaços, apóstrofos e hífens.")));

		mockMvc.perform(post("/pacientes").session(sessao).with(csrf())
				.param("nome", "Paciente Válido")
				.param("cpf", "11144477735")
				.param("dataNascimento", "275760-02-25")
				.param("telefone", "14988888888")
				.param("endereco", "Rua das Flores, 10")
				.param("localTratamento", "Hospital 9 de Julho"))
			.andExpect(status().isOk())
			.andExpect(view().name("pacientes/novo"))
			.andExpect(model().attributeHasFieldErrors("novoPacienteForm", "dataNascimento"))
			.andExpect(content().string(containsString("Informe uma data de nascimento válida.")));

		mockMvc.perform(post("/pacientes").session(sessao).with(csrf())
				.param("nome", "Paciente Válido")
				.param("cpf", "11144477735")
				.param("dataNascimento", "2000-01-01")
				.param("telefone", "14988888888")
				.param("endereco", " A    ")
				.param("localTratamento", " X "))
			.andExpect(status().isOk())
			.andExpect(view().name("pacientes/novo"))
			.andExpect(model().attributeHasFieldErrors("novoPacienteForm", "endereco", "localTratamento"))
			.andExpect(content().string(containsString("Informe um endereço com pelo menos 5 caracteres.")))
			.andExpect(content().string(containsString("Informe o nome da unidade ou cidade de tratamento.")));

		String enderecoCompleto = "Rua das Flores, 125, Centro, Ribeirão do Sul - SP";
		String localTratamentoCompleto = "Hospital Regional de Assis";
		mockMvc.perform(post("/pacientes").session(sessao).with(csrf())
				.param("nome", "Paciente Válido")
				.param("cpf", "11144477735")
				.param("dataNascimento", "2000-01-01")
				.param("telefone", "14988888888")
				.param("endereco", enderecoCompleto)
				.param("localTratamento", localTratamentoCompleto))
			.andExpect(status().isFound());

		var pacienteCadastrado = pacienteRepository.findAll()
			.stream()
			.filter(paciente -> "11144477735".equals(paciente.getCpf()))
			.findFirst()
			.orElseThrow();
		assertThat(pacienteCadastrado.getEndereco()).isEqualTo(enderecoCompleto);
		assertThat(pacienteCadastrado.getLocalTratamento()).isEqualTo(localTratamentoCompleto);

		mockMvc.perform(get("/inicio").session(sessao))
			.andExpect(status().isOk())
			.andExpect(view().name("inicio"))
			.andExpect(model().attributeExists("usuarioLogado", "caminhoAtual", "resumo"))
			.andExpect(content().string(containsString("Pacientes ativos")))
			.andExpect(content().string(containsString("/images/logo-apecan.png")))
			.andExpect(content().string(containsString("Administrador Web")));

		mockMvc.perform(get("/usuarios").session(sessao))
			.andExpect(status().isOk())
			.andExpect(view().name("usuarios/lista"))
			.andExpect(content().string(containsString("Administração")))
			.andExpect(content().string(containsString("Cadastrar funcionário")));

		mockMvc.perform(get("/usuarios/" + resultado.usuario().id()).session(sessao))
			.andExpect(status().isOk())
			.andExpect(view().name("usuarios/detalhe"));

		mockMvc.perform(get("/usuarios/" + resultado.usuario().id() + "/editar").session(sessao))
			.andExpect(status().isOk())
			.andExpect(view().name("usuarios/editar"));

		mockMvc.perform(get("/usuarios/novo").session(sessao))
			.andExpect(status().isOk())
			.andExpect(view().name("usuarios/novo"));

		mockMvc.perform(get("/logout").session(sessao))
			.andExpect(status().isNotFound())
			.andExpect(authenticated().withUsername("admin.web"));

		mockMvc
			.perform(post("/usuarios").session(sessao)
				.with(csrf())
				.param("nome", "Funcionario de Teste")
				.param("login", "funcionario.teste")
				.param("cpf", "11144477735")
				.param("email", "funcionario@apecan.org.br")
				.param("telefone", "14988888888"))
			.andExpect(status().isOk())
			.andExpect(view().name("usuarios/ativacao-local"));
	}

	@Test
	void loginEAtivacaoDevemPossuirPaginasPublicasELogoutGetNaoDeveExistir() throws Exception {
		mockMvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(view().name("login"))
			.andExpect(content().string(containsString("/images/logo-apecan.png")))
			.andExpect(content().string(containsString("/js/login.js")))
			.andExpect(content().string(containsString("data-alternar-senha")))
			.andExpect(content().string(containsString("aria-controls=\"senha\"")));

		mockMvc.perform(get("/ativar-conta")).andExpect(status().isOk()).andExpect(view().name("ativar-conta"));

		mockMvc.perform(get("/esqueci-senha")).andExpect(status().isOk()).andExpect(view().name("esqueci-senha"));

		mockMvc.perform(post("/esqueci-senha").with(csrf()).param("email", "nao-existe@example.invalid"))
			.andExpect(status().isOk())
			.andExpect(view().name("esqueci-senha"));

		mockMvc.perform(post("/esqueci-senha").param("email", "nao-existe@example.invalid"))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/logout")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
	}

	private NovoUsuarioForm formulario() {
		NovoUsuarioForm formulario = new NovoUsuarioForm();
		formulario.setNome("Administrador Web");
		formulario.setLogin("admin.web");
		formulario.setCpf("52998224725");
		formulario.setEmail("admin.web@apecan.org.br");
		formulario.setTelefone("14999999999");
		return formulario;
	}

}
