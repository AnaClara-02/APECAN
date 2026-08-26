package com.aclg.apecan.doacao.service;

import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.doacao.dto.*;
import com.aclg.apecan.doacao.entity.*;
import com.aclg.apecan.doacao.repository.*;
import com.aclg.apecan.equipamento.entity.*;
import com.aclg.apecan.equipamento.repository.*;
import com.aclg.apecan.financeiro.entity.*;
import com.aclg.apecan.financeiro.repository.MovimentacaoFinanceiraRepository;
import com.aclg.apecan.shared.exception.*;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.voluntario.entity.*;
import com.aclg.apecan.voluntario.repository.VoluntarioRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
public class DoacaoService {

	private final DoacaoRepository doacoes;

	private final DoacaoVoluntarioRepository vinculos;

	private final MovimentacaoFinanceiraRepository movimentos;

	private final EquipamentoRepository equipamentos;

	private final CategoriaEquipamentoRepository categorias;

	private final VoluntarioRepository voluntarios;

	private final UsuarioRepository usuarios;

	private final UsuarioAtual atual;

	public DoacaoService(DoacaoRepository d, DoacaoVoluntarioRepository v, MovimentacaoFinanceiraRepository m,
			EquipamentoRepository e, CategoriaEquipamentoRepository c, VoluntarioRepository vo, UsuarioRepository u,
			UsuarioAtual a) {
		doacoes = d;
		vinculos = v;
		movimentos = m;
		equipamentos = e;
		categorias = c;
		voluntarios = vo;
		usuarios = u;
		atual = a;
	}

	@Transactional(readOnly = true)
	public Page<DoacaoDto> listar(TipoDoacao tipo, LocalDate inicio, LocalDate fim, int pagina) {
		return doacoes
			.pesquisar(tipo, inicio, fim, PageRequest.of(Math.max(0, pagina), 20, Sort.by("dataDoacao").descending()))
			.map(this::dto);
	}

	@Transactional(readOnly = true)
	public List<Voluntario> voluntarios() {
		return voluntarios.findAll(Sort.by("nome"));
	}

	@Transactional(readOnly = true)
	public List<CategoriaEquipamento> categorias() {
		return categorias.findAllByOrderByNomeAsc();
	}

	@Transactional
	public Long registrar(@Valid DoacaoForm f) {
		Usuario u = responsavel();
		Doacao d = switch (f.getTipo()) {
			case MONETARIA -> Doacao.monetaria(f.getDataDoacao(), texto(f.getFonteDoacao()), u);
			case EQUIPAMENTO -> Doacao.equipamento(f.getDataDoacao(), texto(f.getFonteDoacao()), u);
			case OUTRO_BEM -> Doacao.outroBem(f.getQuantidade(), texto(f.getUnidade()), f.getDataDoacao(),
					texto(f.getFonteDoacao()), u);
		};
		validar(f);
		doacoes.saveAndFlush(d);
		if (f.getTipo() == TipoDoacao.MONETARIA)
			movimentos.save(new MovimentacaoFinanceira(TipoMovimentacao.ENTRADA, f.getValor(), f.getDataDoacao(),
					"APECAN", OrigemMovimentacao.DOACAO, "Doacao monetaria", d, u));
		if (f.getTipo() == TipoDoacao.EQUIPAMENTO) {
			CategoriaEquipamento c = categorias.findById(f.getCategoriaId())
				.orElseThrow(() -> new RecursoNaoEncontradoException("CATEGORIA_NAO_ENCONTRADA",
						"Categoria nao encontrada."));
			for (int i = 0; i < f.getQuantidadeEquipamentos(); i++)
				equipamentos.save(new Equipamento(c, d, f.getEstadoConservacao(), u));
		}
		vincular(d, f.getDoadorId(), PapelVoluntario.DOADOR);
		vincular(d, f.getResponsavelId(), PapelVoluntario.RESPONSAVEL);
		return d.getId();
	}

	private void validar(DoacaoForm f) {
		if (f.getTipo() == TipoDoacao.MONETARIA && (f.getValor() == null || f.getValor().signum() <= 0))
			throw new OperacaoInvalidaException("VALOR_DOACAO_OBRIGATORIO", "Informe o valor da doacao monetaria.");
		if (f.getTipo() == TipoDoacao.EQUIPAMENTO && (f.getCategoriaId() == null || f.getEstadoConservacao() == null
				|| f.getQuantidadeEquipamentos() == null))
			throw new OperacaoInvalidaException("EQUIPAMENTOS_DOACAO_OBRIGATORIOS",
					"Informe categoria, conservacao e quantidade.");
		if (f.getTipo() == TipoDoacao.OUTRO_BEM
				&& (f.getQuantidade() == null || f.getUnidade() == null || f.getUnidade().isBlank()))
			throw new OperacaoInvalidaException("BENS_DOACAO_OBRIGATORIOS", "Informe quantidade e unidade.");
	}

	private void vincular(Doacao d, Long id, PapelVoluntario papel) {
		if (id == null)
			return;
		Voluntario v = voluntarios.findById(id)
			.orElseThrow(
					() -> new RecursoNaoEncontradoException("VOLUNTARIO_NAO_ENCONTRADO", "Voluntario nao encontrado."));
		vinculos.save(new DoacaoVoluntario(d, v, papel));
	}

	private Usuario responsavel() {
		return usuarios.findById(atual.exigirId()).orElseThrow();
	}

	private String texto(String s) {
		if (s == null || s.isBlank())
			throw new OperacaoInvalidaException("CAMPO_OBRIGATORIO", "Preencha os campos obrigatorios.");
		return s.trim();
	}

	private DoacaoDto dto(Doacao d) {
		var m = movimentos.findByDoacaoId(d.getId()).orElse(null);
		return new DoacaoDto(d.getId(), d.getTipo(), d.getDataDoacao(), d.getFonteDoacao(), d.getQuantidade(),
				d.getUnidade(), (int) equipamentos.countByDoacaoId(d.getId()), m == null ? null : m.getValor());
	}

}
