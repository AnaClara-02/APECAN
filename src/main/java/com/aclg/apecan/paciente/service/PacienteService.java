package com.aclg.apecan.paciente.service;

import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.paciente.dto.AlterarStatusPacienteForm;
import com.aclg.apecan.paciente.dto.EditarPacienteForm;
import com.aclg.apecan.paciente.dto.NovoPacienteForm;
import com.aclg.apecan.paciente.dto.PacienteDetalheResultado;
import com.aclg.apecan.paciente.dto.PacienteResumoDto;
import com.aclg.apecan.paciente.entity.HistoricoStatusPaciente;
import com.aclg.apecan.paciente.entity.Paciente;
import com.aclg.apecan.paciente.entity.StatusPaciente;
import com.aclg.apecan.paciente.mapper.PacienteMapper;
import com.aclg.apecan.paciente.repository.HistoricoStatusPacienteRepository;
import com.aclg.apecan.paciente.repository.PacienteRepository;
import com.aclg.apecan.shared.exception.ConflitoNegocioException;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.shared.exception.RecursoNaoEncontradoException;
import com.aclg.apecan.shared.validation.CpfNormalizer;
import com.aclg.apecan.shared.validation.TelefoneNormalizer;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Validated
public class PacienteService {

	private static final int TAMANHO_PAGINA = 20;

	private final PacienteRepository pacienteRepository;

	private final HistoricoStatusPacienteRepository historicoRepository;

	private final UsuarioRepository usuarioRepository;

	private final PacienteMapper mapper;

	private final UsuarioAtual usuarioAtual;

	private final Clock clock;

	public PacienteService(PacienteRepository pacienteRepository, HistoricoStatusPacienteRepository historicoRepository,
			UsuarioRepository usuarioRepository, PacienteMapper mapper, UsuarioAtual usuarioAtual, Clock clock) {
		this.pacienteRepository = pacienteRepository;
		this.historicoRepository = historicoRepository;
		this.usuarioRepository = usuarioRepository;
		this.mapper = mapper;
		this.usuarioAtual = usuarioAtual;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<PacienteResumoDto> listar(String nome, String cpf, StatusPaciente status, int pagina) {
		String nomeNormalizado = nome == null ? "" : nome.trim();
		String cpfParcial = cpf == null ? "" : cpf.replaceAll("\\D", "");
		PageRequest paginacao = PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA, Sort.by("nome").ascending());
		return pacienteRepository.pesquisar(nomeNormalizado, cpfParcial, status, paginacao).map(mapper::paraResumo);
	}

	@Transactional
	public Long cadastrar(@Valid NovoPacienteForm formulario) {
		Usuario responsavel = usuarioAtual();
		String cpf = CpfNormalizer.normalizar(formulario.getCpf());
		if (pacienteRepository.existsByCpf(cpf)) {
			throw cpfDuplicado();
		}

		Paciente paciente = new Paciente(texto(formulario.getNome()), cpf, formulario.getDataNascimento(),
				TelefoneNormalizer.normalizar(formulario.getTelefone()), texto(formulario.getEndereco()),
				texto(formulario.getLocalTratamento()), responsavel, LocalDate.now(clock));
		try {
			pacienteRepository.saveAndFlush(paciente);
		}
		catch (DataIntegrityViolationException exception) {
			throw cpfDuplicado();
		}

		historicoRepository.save(new HistoricoStatusPaciente(paciente, StatusPaciente.ATIVO, LocalDateTime.now(clock),
				responsavel, "Cadastro do paciente."));
		return paciente.getId();
	}

	@Transactional(readOnly = true)
	public PacienteDetalheResultado buscar(Long id) {
		Paciente paciente = buscarEntidade(id);
		return new PacienteDetalheResultado(mapper.paraDetalhe(paciente),
				historicoRepository.findAllByPacienteIdOrderByAlteradoEmDesc(id)
					.stream()
					.map(mapper::paraHistorico)
					.toList());
	}

	@Transactional(readOnly = true)
	public EditarPacienteForm formularioEdicao(Long id) {
		return mapper.paraFormulario(buscarEntidade(id));
	}

	@Transactional
	public void atualizar(Long id, @Valid EditarPacienteForm formulario) {
		Usuario responsavel = usuarioAtual();
		Paciente paciente = buscarParaAtualizacao(id);
		paciente.atualizarDados(texto(formulario.getNome()), formulario.getDataNascimento(),
				TelefoneNormalizer.normalizar(formulario.getTelefone()), texto(formulario.getEndereco()),
				texto(formulario.getLocalTratamento()), responsavel, LocalDate.now(clock));
		pacienteRepository.flush();
	}

	@Transactional
	public void alterarStatus(Long id, @Valid AlterarStatusPacienteForm formulario) {
		Usuario responsavel = usuarioAtual();
		Paciente paciente = buscarParaAtualizacao(id);
		StatusPaciente atual = paciente.getStatus();
		StatusPaciente novo = formulario.getStatus();

		if (atual == StatusPaciente.FALECIDO) {
			throw new OperacaoInvalidaException("STATUS_FALECIDO_TERMINAL",
					"O status falecido nao pode ser alterado pela operacao normal.");
		}
		if (atual == novo) {
			throw new OperacaoInvalidaException("STATUS_PACIENTE_IGUAL", "Selecione um status diferente do atual.");
		}

		LocalDateTime agora = LocalDateTime.now(clock);
		paciente.alterarStatus(novo, responsavel, agora);
		historicoRepository
			.save(new HistoricoStatusPaciente(paciente, novo, agora, responsavel, texto(formulario.getObservacao())));
		pacienteRepository.flush();
	}

	private Usuario usuarioAtual() {
		Long id = usuarioAtual.exigirId();
		return usuarioRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("USUARIO_ATUAL_NAO_ENCONTRADO",
					"O usuario autenticado nao foi encontrado."));
	}

	private Paciente buscarEntidade(Long id) {
		return pacienteRepository.findById(id).orElseThrow(() -> naoEncontrado(id));
	}

	private Paciente buscarParaAtualizacao(Long id) {
		return pacienteRepository.findByIdForUpdate(id).orElseThrow(() -> naoEncontrado(id));
	}

	private RecursoNaoEncontradoException naoEncontrado(Long id) {
		return new RecursoNaoEncontradoException("PACIENTE_NAO_ENCONTRADO", "Paciente " + id + " nao encontrado.");
	}

	private ConflitoNegocioException cpfDuplicado() {
		return new ConflitoNegocioException("CPF_PACIENTE_JA_CADASTRADO", "Ja existe um paciente com o CPF informado.");
	}

	private String texto(String valor) {
		if (valor == null || valor.isBlank()) {
			throw new OperacaoInvalidaException("CAMPO_OBRIGATORIO", "Preencha todos os campos obrigatorios.");
		}
		return valor.trim();
	}

}
