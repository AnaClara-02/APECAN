package com.aclg.apecan.integracao;

import com.aclg.apecan.auth.security.UsuarioPrincipal;
import com.aclg.apecan.auth.service.AtivacaoUsuarioService;
import com.aclg.apecan.despesa.dto.DespesaForm;
import com.aclg.apecan.despesa.repository.DespesaRepository;
import com.aclg.apecan.doacao.dto.DoacaoForm;
import com.aclg.apecan.doacao.entity.TipoDoacao;
import com.aclg.apecan.doacao.service.DoacaoService;
import com.aclg.apecan.emprestimo.dto.DevolucaoForm;
import com.aclg.apecan.emprestimo.dto.EmprestimoForm;
import com.aclg.apecan.emprestimo.service.EmprestimoService;
import com.aclg.apecan.equipamento.dto.CategoriaEquipamentoForm;
import com.aclg.apecan.equipamento.dto.EquipamentoForm;
import com.aclg.apecan.equipamento.entity.EstadoConservacao;
import com.aclg.apecan.equipamento.entity.StatusEquipamento;
import com.aclg.apecan.equipamento.repository.EquipamentoRepository;
import com.aclg.apecan.equipamento.service.EquipamentoService;
import com.aclg.apecan.financeiro.entity.TipoMovimentacao;
import com.aclg.apecan.financeiro.repository.MovimentacaoFinanceiraRepository;
import com.aclg.apecan.financeiro.service.FinanceiroService;
import com.aclg.apecan.paciente.dto.NovoPacienteForm;
import com.aclg.apecan.paciente.service.PacienteService;
import com.aclg.apecan.usuario.dto.NovoUsuarioForm;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.usuario.service.UsuarioService;
import com.aclg.apecan.voluntario.dto.VoluntarioForm;
import com.aclg.apecan.voluntario.service.VoluntarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ModulosOperacionaisServiceTests {

	@Autowired
	UsuarioService usuarioService;

	@Autowired
	AtivacaoUsuarioService ativacaoService;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PacienteService pacientes;

	@Autowired
	VoluntarioService voluntarios;

	@Autowired
	EquipamentoService equipamentosService;

	@Autowired
	EmprestimoService emprestimos;

	@Autowired
	DoacaoService doacoes;

	@Autowired
	FinanceiroService financeiro;

	@Autowired
	EquipamentoRepository equipamentos;

	@Autowired
	MovimentacaoFinanceiraRepository movimentos;

	@Autowired
	DespesaRepository despesas;

	@AfterEach
	void limpar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void deveExecutarFluxosAtomicosDeEstoqueEmprestimoDoacaoEDespesa() {
		autenticar(administrador());
		Long pacienteId = pacientes.cadastrar(paciente());
		Long voluntarioId = voluntarios.cadastrar(voluntario());
		CategoriaEquipamentoForm cf = new CategoriaEquipamentoForm();
		cf.setNome("Cadeira de rodas");
		Long categoriaId = equipamentosService.cadastrarCategoria(cf);
		EquipamentoForm ef = new EquipamentoForm();
		ef.setCategoriaId(categoriaId);
		ef.setEstadoConservacao(EstadoConservacao.BOM);
		Long equipamentoId = equipamentosService.cadastrar(ef);

		EmprestimoForm emprestimo = new EmprestimoForm();
		emprestimo.setEquipamentoId(equipamentoId);
		emprestimo.setPacienteId(pacienteId);
		emprestimo.setDataEmprestimo(LocalDate.now());
		emprestimo.setDataPrevistaDevolucao(LocalDate.now().plusDays(7));
		Long emprestimoId = emprestimos.emprestar(emprestimo);
		assertThat(equipamentos.findById(equipamentoId).orElseThrow().getStatus())
			.isEqualTo(StatusEquipamento.EMPRESTADO);
		DevolucaoForm devolucao = new DevolucaoForm();
		devolucao.setDataDevolucao(LocalDate.now());
		devolucao.setEstadoConservacao(EstadoConservacao.REGULAR);
		emprestimos.devolver(emprestimoId, devolucao);
		assertThat(equipamentos.findById(equipamentoId).orElseThrow().getStatus()).isEqualTo(StatusEquipamento.ATIVO);

		DoacaoForm monetaria = baseDoacao(TipoDoacao.MONETARIA, voluntarioId);
		monetaria.setValor(new BigDecimal("125.50"));
		doacoes.registrar(monetaria);
		assertThat(movimentos.count()).isEqualTo(1);
		assertThat(movimentos.findAll().getFirst().getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
		long antes = equipamentos.count();
		DoacaoForm equipamento = baseDoacao(TipoDoacao.EQUIPAMENTO, voluntarioId);
		equipamento.setCategoriaId(categoriaId);
		equipamento.setEstadoConservacao(EstadoConservacao.NOVO);
		equipamento.setQuantidadeEquipamentos(3);
		doacoes.registrar(equipamento);
		assertThat(equipamentos.count()).isEqualTo(antes + 3);

		DespesaForm despesa = new DespesaForm();
		despesa.setTipo("Energia");
		despesa.setDescricao("Conta mensal");
		despesa.setNumeroNotaFiscal("NF-2026-01");
		despesa.setValor(new BigDecimal("50.00"));
		despesa.setData(LocalDate.now());
		despesa.setDestino("Concessionaria");
		financeiro.registrarDespesa(despesa);
		assertThat(despesas.count()).isEqualTo(1);
		assertThat(movimentos.count()).isEqualTo(2);
		assertThat(financeiro.saldo(null, null)).isEqualByComparingTo("75.50");
	}

	private Usuario administrador() {
		NovoUsuarioForm f = new NovoUsuarioForm();
		f.setNome("Admin Integracao");
		f.setLogin("admin.integracao");
		f.setCpf("52998224725");
		f.setEmail("admin.integracao@example.invalid");
		f.setTelefone("14999999999");
		var r = usuarioService.cadastrarPrimeiroAdministrador(f);
		String token = URI.create(r.ativacao().linkLocal()).getRawQuery().substring(6);
		ativacaoService.ativar(token, "Senha integracao 2026", "Senha integracao 2026");
		return usuarios.findByLogin("admin.integracao").orElseThrow();
	}

	private void autenticar(Usuario u) {
		var p = UsuarioPrincipal.de(u);
		SecurityContextHolder.getContext()
			.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities()));
	}

	private NovoPacienteForm paciente() {
		NovoPacienteForm f = new NovoPacienteForm();
		f.setNome("Paciente Integracao");
		f.setCpf("11144477735");
		f.setDataNascimento(LocalDate.of(1990, 1, 1));
		f.setTelefone("14988887777");
		f.setEndereco("Endereco");
		f.setLocalTratamento("Hospital");
		return f;
	}

	private VoluntarioForm voluntario() {
		VoluntarioForm f = new VoluntarioForm();
		f.setNome("Voluntario Integracao");
		f.setCpf("12345678909");
		f.setDataNascimento(LocalDate.of(1990, 1, 1));
		f.setTelefone("14977776666");
		f.setEndereco("Endereco");
		return f;
	}

	private DoacaoForm baseDoacao(TipoDoacao tipo, Long voluntarioId) {
		DoacaoForm f = new DoacaoForm();
		f.setTipo(tipo);
		f.setDataDoacao(LocalDate.now());
		f.setFonteDoacao("Doador ficticio");
		f.setDoadorId(voluntarioId);
		return f;
	}

}
