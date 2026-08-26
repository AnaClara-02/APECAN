package com.aclg.apecan.paciente.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aclg.apecan.auth.security.UsuarioPrincipal;
import com.aclg.apecan.auth.service.AtivacaoUsuarioService;
import com.aclg.apecan.paciente.dto.AlterarStatusPacienteForm;
import com.aclg.apecan.paciente.dto.EditarPacienteForm;
import com.aclg.apecan.paciente.dto.NovoPacienteForm;
import com.aclg.apecan.paciente.entity.StatusPaciente;
import com.aclg.apecan.paciente.repository.HistoricoStatusPacienteRepository;
import com.aclg.apecan.paciente.repository.PacienteRepository;
import com.aclg.apecan.shared.exception.ConflitoNegocioException;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.dto.UsuarioCriadoResultado;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.usuario.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;

@SpringBootTest
@Transactional
class PacienteServiceTests {

	private static final String SENHA = "Senha paciente testes 2026";

	@Autowired
	private PacienteService pacienteService;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private HistoricoStatusPacienteRepository historicoRepository;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private AtivacaoUsuarioService ativacaoService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@AfterEach
	void limparAutenticacao() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void deveCadastrarEditarEManterHistoricoSemExcluirPaciente() {
		autenticar(criarAdministrador());
		NovoPacienteForm novo = novoPaciente();
		Long id = pacienteService.cadastrar(novo);

		assertThat(pacienteRepository.findById(id).orElseThrow().getCpf()).isEqualTo("11144477735");
		assertThat(pacienteService.listar("", "", null, 0).getContent().getFirst().cpfMascarado())
			.isEqualTo("***.***.***-35");
		assertThat(historicoRepository.findAllByPacienteIdOrderByAlteradoEmDesc(id)).hasSize(1)
			.first()
			.extracting("status")
			.isEqualTo(StatusPaciente.ATIVO);

		EditarPacienteForm editar = new EditarPacienteForm();
		editar.setNome("Paciente Atualizado");
		editar.setDataNascimento(LocalDate.of(1990, 5, 10));
		editar.setTelefone("(14) 98888-7777");
		editar.setEndereco("Endereco atualizado");
		editar.setLocalTratamento("Hospital atualizado");
		pacienteService.atualizar(id, editar);
		assertThat(pacienteRepository.findById(id).orElseThrow().getAtualizadoPor()).isNotNull();

		alterarStatus(id, StatusPaciente.INATIVO, "Tratamento temporariamente suspenso.");
		alterarStatus(id, StatusPaciente.ATIVO, "Tratamento retomado.");
		alterarStatus(id, StatusPaciente.FALECIDO, "Informacao confirmada pela associacao.");

		assertThat(historicoRepository.findAllByPacienteIdOrderByAlteradoEmDesc(id)).hasSize(4);
		assertThatThrownBy(() -> alterarStatus(id, StatusPaciente.ATIVO, "Tentativa de reversao."))
			.isInstanceOf(OperacaoInvalidaException.class)
			.hasMessageContaining("nao pode ser alterado");

		assertThatThrownBy(() -> pacienteService.cadastrar(novo)).isInstanceOf(ConflitoNegocioException.class);
	}

	private Usuario criarAdministrador() {
		NovoUsuarioForm form = new NovoUsuarioForm();
		form.setNome("Administrador Pacientes");
		form.setLogin("admin.pacientes");
		form.setCpf("52998224725");
		form.setEmail("admin.pacientes@example.invalid");
		form.setTelefone("14999999999");
		UsuarioCriadoResultado resultado = usuarioService.cadastrarPrimeiroAdministrador(form);
		String token = URI.create(resultado.ativacao().linkLocal()).getRawQuery().substring("token=".length());
		ativacaoService.ativar(token, SENHA, SENHA);
		return usuarioRepository.findByLogin("admin.pacientes").orElseThrow();
	}

	private void autenticar(Usuario usuario) {
		UsuarioPrincipal principal = UsuarioPrincipal.de(usuario);
		SecurityContextHolder.getContext()
			.setAuthentication(
					UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
	}

	private NovoPacienteForm novoPaciente() {
		NovoPacienteForm form = new NovoPacienteForm();
		form.setNome("Paciente de Teste");
		form.setCpf("111.444.777-35");
		form.setDataNascimento(LocalDate.of(1990, 5, 10));
		form.setTelefone("(14) 98888-7777");
		form.setEndereco("Endereco de teste");
		form.setLocalTratamento("Hospital de teste");
		return form;
	}

	private void alterarStatus(Long id, StatusPaciente status, String observacao) {
		AlterarStatusPacienteForm form = new AlterarStatusPacienteForm();
		form.setStatus(status);
		form.setObservacao(observacao);
		pacienteService.alterarStatus(id, form);
	}

}
