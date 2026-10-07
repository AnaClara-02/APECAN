package com.aclg.apecan.voluntario.service;

import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.shared.exception.ConflitoNegocioException;
import com.aclg.apecan.shared.exception.RecursoNaoEncontradoException;
import com.aclg.apecan.shared.validation.CpfFormatter;
import com.aclg.apecan.shared.validation.CpfNormalizer;
import com.aclg.apecan.shared.validation.TelefoneNormalizer;
import com.aclg.apecan.shared.validation.TelefoneFormatter;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.voluntario.dto.VoluntarioDto;
import com.aclg.apecan.voluntario.dto.VoluntarioForm;
import com.aclg.apecan.voluntario.entity.StatusVoluntario;
import com.aclg.apecan.voluntario.entity.Voluntario;
import com.aclg.apecan.voluntario.repository.VoluntarioRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@org.springframework.validation.annotation.Validated
public class VoluntarioService {

	private final VoluntarioRepository repository;

	private final UsuarioRepository usuarios;

	private final UsuarioAtual usuarioAtual;

	private final Clock clock;

	public VoluntarioService(VoluntarioRepository r, UsuarioRepository u, UsuarioAtual a, Clock c) {
		repository = r;
		usuarios = u;
		usuarioAtual = a;
		clock = c;
	}

	@Transactional(readOnly = true)
	public Page<VoluntarioDto> listar(String nome, StatusVoluntario status, int pagina) {
		return repository
			.pesquisar(nome == null ? "" : nome.trim(), status,
					PageRequest.of(Math.max(0, pagina), 20, Sort.by("nome")))
			.map(this::dto);
	}

	@Transactional
	public Long cadastrar(@Valid VoluntarioForm f) {
		String cpf = CpfNormalizer.normalizar(f.getCpf());
		if (repository.existsByCpf(cpf))
			throw new ConflitoNegocioException("CPF_VOLUNTARIO_JA_CADASTRADO", "Ja existe voluntario com esse CPF.");
		Voluntario v = new Voluntario(f.getNome().trim(), cpf, f.getDataNascimento(),
				TelefoneNormalizer.normalizar(f.getTelefone()), f.getEndereco().trim(), responsavel(),
				LocalDate.now(clock));
		repository.saveAndFlush(v);
		return v.getId();
	}

	@Transactional(readOnly = true)
	public VoluntarioDto buscar(Long id) {
		return dto(entidade(id));
	}

	@Transactional(readOnly = true)
	public VoluntarioForm formulario(Long id) {
		Voluntario v = entidade(id);
		VoluntarioForm f = new VoluntarioForm();
		f.setNome(v.getNome());
		f.setCpf(v.getCpf());
		f.setDataNascimento(v.getDataNascimento());
		f.setTelefone(TelefoneFormatter.formatar(v.getTelefone()));
		f.setEndereco(v.getEndereco());
		return f;
	}

	@Transactional
	public void atualizar(Long id, @Valid VoluntarioForm f) {
		Voluntario v = paraAtualizar(id);
		v.atualizarDados(f.getNome().trim(), f.getDataNascimento(), TelefoneNormalizer.normalizar(f.getTelefone()),
				f.getEndereco().trim(), responsavel(), LocalDate.now(clock));
	}

	@Transactional
	public void alterarStatus(Long id) {
		Voluntario v = paraAtualizar(id);
		Usuario u = responsavel();
		if (v.getStatus() == StatusVoluntario.ATIVO)
			v.desativar(u, LocalDateTime.now(clock));
		else
			v.reativar(u);
	}

	private Voluntario entidade(Long id) {
		return repository.findById(id)
			.orElseThrow(
					() -> new RecursoNaoEncontradoException("VOLUNTARIO_NAO_ENCONTRADO", "Voluntario nao encontrado."));
	}

	private Voluntario paraAtualizar(Long id) {
		return repository.findByIdForUpdate(id)
			.orElseThrow(
					() -> new RecursoNaoEncontradoException("VOLUNTARIO_NAO_ENCONTRADO", "Voluntario nao encontrado."));
	}

	private Usuario responsavel() {
		return usuarios.findById(usuarioAtual.exigirId()).orElseThrow();
	}

	private VoluntarioDto dto(Voluntario v) {
		return new VoluntarioDto(v.getId(), v.getNome(), CpfFormatter.mascarar(v.getCpf()), v.getDataNascimento(),
				TelefoneFormatter.formatar(v.getTelefone()), v.getEndereco(), v.getStatus());
	}

}
