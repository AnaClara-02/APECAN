package com.aclg.apecan.emprestimo.controller;

import com.aclg.apecan.auth.security.UsuarioPrincipal;
import com.aclg.apecan.auth.service.AtivacaoUsuarioService;
import com.aclg.apecan.emprestimo.repository.EmprestimoEquipamentoRepository;
import com.aclg.apecan.equipamento.dto.CategoriaEquipamentoForm;
import com.aclg.apecan.equipamento.dto.EquipamentoForm;
import com.aclg.apecan.equipamento.entity.EstadoConservacao;
import com.aclg.apecan.equipamento.entity.StatusEquipamento;
import com.aclg.apecan.equipamento.repository.EquipamentoRepository;
import com.aclg.apecan.equipamento.service.EquipamentoService;
import com.aclg.apecan.paciente.dto.NovoPacienteForm;
import com.aclg.apecan.paciente.service.PacienteService;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@Transactional
class EmprestimoControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext applicationContext;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private AtivacaoUsuarioService ativacaoService;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PacienteService pacientes;

	@Autowired
	private EquipamentoService equipamentosService;

	@Autowired
	private EquipamentoRepository equipamentos;

	@Autowired
	private EmprestimoEquipamentoRepository emprestimos;

	@BeforeEach
	void preparar() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).apply(springSecurity()).build();
	}

	@Test
	void deveRegistrarEmprestimoEEmitirFeedback() throws Exception {
		Usuario administrador = administrador();
		var autenticacao = autenticacao(administrador);
		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(autenticacao);
		Long pacienteId = pacientes.cadastrar(paciente());
		Long equipamentoId = equipamento();

		mockMvc.perform(post("/emprestimos").with(authentication(autenticacao)).with(csrf())
			.param("equipamentoId", equipamentoId.toString())
			.param("pacienteId", pacienteId.toString())
			.param("dataEmprestimo", LocalDate.now().toString())
			.param("dataPrevistaDevolucao", LocalDate.now().plusDays(7).toString())
			.param("observacao", "Emprestimo de teste"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("/emprestimos/*"))
			.andExpect(flash().attribute("sucesso", "Emprestimo registrado."));

		assertThat(emprestimos.count()).isEqualTo(1);
		assertThat(equipamentos.findById(equipamentoId).orElseThrow().getStatus())
			.isEqualTo(StatusEquipamento.EMPRESTADO);
	}

	@Test
	void deveMostrarErrosQuandoFormularioForInvalido() throws Exception {
		Usuario administrador = administrador();
		var autenticacao = autenticacao(administrador);

		mockMvc.perform(post("/emprestimos").with(authentication(autenticacao)).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(view().name("emprestimos/novo"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Nao foi possivel registrar")));

		assertThat(emprestimos.count()).isZero();
	}

	private UsernamePasswordAuthenticationToken autenticacao(Usuario usuario) {
		UsuarioPrincipal principal = UsuarioPrincipal.de(usuario);
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
	}

	private Usuario administrador() {
		NovoUsuarioForm formulario = new NovoUsuarioForm();
		formulario.setNome("Admin Emprestimo");
		formulario.setLogin("admin.emprestimo");
		formulario.setCpf("52998224725");
		formulario.setEmail("admin.emprestimo@example.invalid");
		formulario.setTelefone("+55 (14) 99999-9999");
		var resultado = usuarioService.cadastrarPrimeiroAdministrador(formulario);
		String token = URI.create(resultado.ativacao().linkLocal()).getRawQuery().substring("token=".length());
		ativacaoService.ativar(token, "Senha emprestimo 2026", "Senha emprestimo 2026");
		return usuarios.findByLogin("admin.emprestimo").orElseThrow();
	}

	private NovoPacienteForm paciente() {
		NovoPacienteForm formulario = new NovoPacienteForm();
		formulario.setNome("Paciente Emprestimo");
		formulario.setCpf("11144477735");
		formulario.setDataNascimento(LocalDate.of(1990, 1, 1));
		formulario.setTelefone("+55 (14) 98888-7777");
		formulario.setEndereco("Endereco de teste");
		formulario.setLocalTratamento("Hospital de teste");
		return formulario;
	}

	private Long equipamento() {
		CategoriaEquipamentoForm categoria = new CategoriaEquipamentoForm();
		categoria.setNome("Cadeira de teste");
		Long categoriaId = equipamentosService.cadastrarCategoria(categoria);
		EquipamentoForm equipamento = new EquipamentoForm();
		equipamento.setCategoriaId(categoriaId);
		equipamento.setEstadoConservacao(EstadoConservacao.BOM);
		return equipamentosService.cadastrar(equipamento);
	}

}
