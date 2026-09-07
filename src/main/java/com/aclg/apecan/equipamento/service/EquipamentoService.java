package com.aclg.apecan.equipamento.service;

import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.equipamento.dto.*;
import com.aclg.apecan.equipamento.entity.*;
import com.aclg.apecan.equipamento.repository.*;
import com.aclg.apecan.shared.exception.*;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class EquipamentoService {

	private final EquipamentoRepository equipamentos;

	private final CategoriaEquipamentoRepository categorias;

	private final UsuarioRepository usuarios;

	private final UsuarioAtual atual;

	public EquipamentoService(EquipamentoRepository e, CategoriaEquipamentoRepository c, UsuarioRepository u,
			UsuarioAtual a) {
		equipamentos = e;
		categorias = c;
		usuarios = u;
		atual = a;
	}

	@Transactional(readOnly = true)
	public Page<EquipamentoDto> listar(Long categoria, StatusEquipamento status, int pagina) {
		return equipamentos
			.pesquisar(categoria, status, PageRequest.of(Math.max(0, pagina), 20, Sort.by("id").descending()))
			.map(this::dto);
	}

	@Transactional(readOnly = true)
	public List<CategoriaEquipamento> categorias() {
		return categorias.findAllByOrderByNomeAsc();
	}

	@Transactional(readOnly = true)
	public List<ResumoCategoriaEquipamento> resumo() {
		return equipamentos.resumirPorCategoria();
	}

	@Transactional
	public Long cadastrarCategoria(@Valid CategoriaEquipamentoForm f) {
		String nome = f.getNome().trim();
		if (categorias.existsByNomeIgnoreCase(nome))
			throw new ConflitoNegocioException("CATEGORIA_DUPLICADA", "Ja existe categoria com esse nome.");
		CategoriaEquipamento c = new CategoriaEquipamento(nome, limpar(f.getDescricao()), responsavel());
		categorias.saveAndFlush(c);
		return c.getId();
	}

	@Transactional
	public Long cadastrar(@Valid EquipamentoForm f) {
		Equipamento e = new Equipamento(categoriaParaAtualizar(f.getCategoriaId()), null, f.getEstadoConservacao(), responsavel());
		equipamentos.saveAndFlush(e);
		return e.getId();
	}

	@Transactional
	public void excluirCategoria(Long id) {
		CategoriaEquipamento categoria = categoriaParaAtualizar(id);
		if (equipamentos.countByCategoriaId(id) != 0) {
			throw new OperacaoInvalidaException("CATEGORIA_EM_USO", "A categoria possui equipamentos e nao pode ser excluida.");
		}
		try {
			categorias.delete(categoria);
			categorias.flush();
		}
		catch (DataIntegrityViolationException exception) {
			throw new OperacaoInvalidaException("CATEGORIA_EM_USO", "A categoria possui equipamentos e nao pode ser excluida.");
		}
	}

	@Transactional(readOnly = true)
	public EquipamentoDto buscar(Long id) {
		return dto(entidade(id));
	}

	@Transactional(readOnly = true)
	public EquipamentoForm formulario(Long id) {
		Equipamento e = entidade(id);
		EquipamentoForm f = new EquipamentoForm();
		f.setCategoriaId(e.getCategoria().getId());
		f.setEstadoConservacao(e.getEstadoConservacao());
		return f;
	}

	@Transactional
	public void atualizar(Long id, @Valid EquipamentoForm f) {
		Equipamento e = paraAtualizar(id);
		Usuario u = responsavel();
		e.atualizarCategoria(categoria(f.getCategoriaId()), u);
		e.atualizarEstadoConservacao(f.getEstadoConservacao(), u);
	}

	@Transactional
	public void alterarStatus(Long id) {
		Equipamento e = paraAtualizar(id);
		Usuario u = responsavel();
		if (e.getStatus() == StatusEquipamento.ATIVO)
			e.desativar(u);
		else if (e.getStatus() == StatusEquipamento.INATIVO)
			e.reativar(u);
		else
			throw new OperacaoInvalidaException("EQUIPAMENTO_EMPRESTADO",
					"Devolva o equipamento antes de alterar o status.");
	}

	private CategoriaEquipamento categoria(Long id) {
		return categorias.findById(id)
			.orElseThrow(
					() -> new RecursoNaoEncontradoException("CATEGORIA_NAO_ENCONTRADA", "Categoria nao encontrada."));
	}

	private CategoriaEquipamento categoriaParaAtualizar(Long id) {
		return categorias.findByIdForUpdate(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("CATEGORIA_NAO_ENCONTRADA", "Categoria nao encontrada."));
	}

	private Equipamento entidade(Long id) {
		return equipamentos.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("EQUIPAMENTO_NAO_ENCONTRADO",
					"Equipamento nao encontrado."));
	}

	private Equipamento paraAtualizar(Long id) {
		return equipamentos.findByIdForUpdate(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("EQUIPAMENTO_NAO_ENCONTRADO",
					"Equipamento nao encontrado."));
	}

	private Usuario responsavel() {
		return usuarios.findById(atual.exigirId()).orElseThrow();
	}

	private String limpar(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	private EquipamentoDto dto(Equipamento e) {
		return new EquipamentoDto(e.getId(), e.getCategoria().getNome(), e.getEstadoConservacao(), e.getStatus(),
				e.getDoacao() == null ? null : e.getDoacao().getId());
	}

}
