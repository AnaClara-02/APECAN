package com.aclg.apecan.emprestimo.service;

import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.emprestimo.dto.*;
import com.aclg.apecan.emprestimo.entity.EmprestimoEquipamento;
import com.aclg.apecan.emprestimo.repository.EmprestimoEquipamentoRepository;
import com.aclg.apecan.equipamento.entity.*;
import com.aclg.apecan.equipamento.repository.EquipamentoRepository;
import com.aclg.apecan.paciente.entity.*;
import com.aclg.apecan.paciente.repository.PacienteRepository;
import com.aclg.apecan.shared.exception.*;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@org.springframework.validation.annotation.Validated
public class EmprestimoService {

	private final EmprestimoEquipamentoRepository emprestimos;

	private final EquipamentoRepository equipamentos;

	private final PacienteRepository pacientes;

	private final UsuarioRepository usuarios;

	private final UsuarioAtual atual;

	private final Clock clock;

	public EmprestimoService(EmprestimoEquipamentoRepository e, EquipamentoRepository eq, PacienteRepository p,
			UsuarioRepository u, UsuarioAtual a, Clock c) {
		emprestimos = e;
		equipamentos = eq;
		pacientes = p;
		usuarios = u;
		atual = a;
		clock = c;
	}

	@Transactional(readOnly = true)
	public Page<EmprestimoDto> listar(StatusEmprestimoFiltro status, int pagina) {
		Pageable p = PageRequest.of(Math.max(0, pagina), 20, Sort.by("dataEmprestimo").descending());
		Page<EmprestimoEquipamento> resultado = switch (status) {
			case ATIVO -> emprestimos.findAllByDataDevolucaoIsNull(p);
			case INATIVO -> emprestimos.findAllByDataDevolucaoIsNotNull(p);
			case TODOS -> emprestimos.listarTodos(p);
		};
		return resultado.map(this::dto);
	}

	@Transactional(readOnly = true)
	public Page<HistoricoEquipamentoDto> historicoEquipamento(Long equipamentoId, int pagina) {
		Pageable pageable = PageRequest.of(Math.max(0, pagina), 10,
			Sort.by("dataEmprestimo").descending().and(Sort.by("id").descending()));
		return emprestimos.findAllByEquipamentoId(equipamentoId, pageable)
			.map(e -> new HistoricoEquipamentoDto(e.getId(), e.getPaciente().getId(), e.getPaciente().getNome(),
				e.getDataEmprestimo(), e.getDataPrevistaDevolucao(), e.getDataDevolucao(),
				e.getEstadoConservacaoDevolucao(), situacao(e)));
	}

	@Transactional(readOnly = true)
	public List<Equipamento> disponiveis() {
		return equipamentos.findAllByStatusOrderByCategoriaNomeAscIdAsc(StatusEquipamento.ATIVO);
	}

	@Transactional(readOnly = true)
	public List<Paciente> pacientesAtivos() {
		return pacientes.findAllByStatusOrderByNomeAsc(StatusPaciente.ATIVO);
	}

	@Transactional
	public Long emprestar(@Valid EmprestimoForm f) {
		Equipamento eq = equipamentos.findByIdForUpdate(f.getEquipamentoId())
			.orElseThrow(() -> new RecursoNaoEncontradoException("EQUIPAMENTO_NAO_ENCONTRADO",
					"Equipamento nao encontrado."));
		Paciente p = pacientes.findByIdForUpdate(f.getPacienteId())
			.orElseThrow(
					() -> new RecursoNaoEncontradoException("PACIENTE_NAO_ENCONTRADO", "Paciente nao encontrado."));
		if (p.getStatus() != StatusPaciente.ATIVO)
			throw new OperacaoInvalidaException("PACIENTE_INATIVO", "Somente paciente ativo pode receber equipamento.");
		if (emprestimos.existsByEquipamentoIdAndDataDevolucaoIsNull(eq.getId()))
			throw new ConflitoNegocioException("EMPRESTIMO_ABERTO", "O equipamento ja possui emprestimo aberto.");
		Usuario u = responsavel();
		eq.emprestar(u);
		EmprestimoEquipamento e = new EmprestimoEquipamento(eq, p, f.getDataEmprestimo(), f.getDataPrevistaDevolucao(),
				u, limpar(f.getObservacao()));
		try {
			emprestimos.saveAndFlush(e);
		}
		catch (DataIntegrityViolationException exception) {
			throw new ConflitoNegocioException("EMPRESTIMO_CONCORRENTE",
					"O equipamento acabou de ser emprestado em outra operacao. Atualize a pagina e escolha outro.");
		}
		return e.getId();
	}

	@Transactional(readOnly = true)
	public EmprestimoDto buscar(Long id) {
		return dto(entidade(id));
	}

	@Transactional
	public void devolver(Long id, @Valid DevolucaoForm f) {
		EmprestimoEquipamento e = emprestimos.findByIdForUpdate(id).orElseThrow(() -> naoEncontrado());
		Equipamento eq = equipamentos.findByIdForUpdate(e.getEquipamento().getId()).orElseThrow();
		Usuario u = responsavel();
		e.registrarDevolucao(f.getDataDevolucao(), u, f.getEstadoConservacao());
		eq.registrarDevolucao(f.getEstadoConservacao(), u);
		if (f.isInativarEquipamento())
			eq.desativar(u);
	}

	private EmprestimoEquipamento entidade(Long id) {
		return emprestimos.findById(id).orElseThrow(() -> naoEncontrado());
	}

	private RecursoNaoEncontradoException naoEncontrado() {
		return new RecursoNaoEncontradoException("EMPRESTIMO_NAO_ENCONTRADO", "Emprestimo nao encontrado.");
	}

	private Usuario responsavel() {
		return usuarios.findById(atual.exigirId()).orElseThrow();
	}

	private String limpar(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	private EmprestimoDto dto(EmprestimoEquipamento e) {
		boolean atrasado = e.estaAberto() && e.getDataPrevistaDevolucao() != null
				&& e.getDataPrevistaDevolucao().isBefore(LocalDate.now(clock));
		return new EmprestimoDto(e.getId(), e.getEquipamento().getId(), e.getEquipamento().getCategoria().getNome(),
				e.getPaciente().getId(), e.getPaciente().getNome(), e.getDataEmprestimo(), e.getDataPrevistaDevolucao(),
				e.getDataDevolucao(), atrasado, e.getObservacao());
	}

	private String situacao(EmprestimoEquipamento e) {
		if (!e.estaAberto())
			return "INATIVO";
		return e.getDataPrevistaDevolucao() != null && e.getDataPrevistaDevolucao().isBefore(LocalDate.now(clock))
				? "ATRASADO" : "ATIVO";
	}

}
