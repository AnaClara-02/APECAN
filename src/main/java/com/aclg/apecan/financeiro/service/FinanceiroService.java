package com.aclg.apecan.financeiro.service;

import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.despesa.dto.*;
import com.aclg.apecan.despesa.entity.Despesa;
import com.aclg.apecan.despesa.repository.DespesaRepository;
import com.aclg.apecan.financeiro.dto.*;
import com.aclg.apecan.financeiro.entity.*;
import com.aclg.apecan.financeiro.repository.MovimentacaoFinanceiraRepository;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@org.springframework.validation.annotation.Validated
public class FinanceiroService {

	private final MovimentacaoFinanceiraRepository movimentos;

	private final DespesaRepository despesas;

	private final UsuarioRepository usuarios;

	private final UsuarioAtual atual;

	public FinanceiroService(MovimentacaoFinanceiraRepository m, DespesaRepository d, UsuarioRepository u,
			UsuarioAtual a) {
		movimentos = m;
		despesas = d;
		usuarios = u;
		atual = a;
	}

	@Transactional(readOnly = true)
	public Page<MovimentacaoDto> listar(TipoMovimentacao tipo, LocalDate inicio, LocalDate fim, int pagina) {
		validarPeriodo(inicio, fim);
		return movimentos
			.pesquisar(tipo, inicio, fim,
					PageRequest.of(Math.max(0, pagina), 20, Sort.by("dataMovimentacao").descending()))
			.map(this::dto);
	}

	@Transactional(readOnly = true)
	public BigDecimal saldo(LocalDate i, LocalDate f) {
		validarPeriodo(i, f);
		return movimentos.saldo(i, f);
	}

	@Transactional(readOnly = true)
	public Page<DespesaDto> despesas(int pagina) {
		return despesas
			.findAllByOrderByMovimentacaoFinanceiraDataMovimentacaoDesc(PageRequest.of(Math.max(0, pagina), 20))
			.map(this::dto);
	}

	@Transactional
	public Long registrar(@Valid MovimentacaoForm f) {
		MovimentacaoFinanceira m = new MovimentacaoFinanceira(f.getTipo(), f.getValor(), f.getData(),
				f.getDestino().trim(), OrigemMovimentacao.OUTRO_MEIO, f.getDescricao().trim(), null, responsavel());
		movimentos.saveAndFlush(m);
		return m.getId();
	}

	@Transactional
	public Long registrarDespesa(@Valid DespesaForm f) {
		Usuario u = responsavel();
		MovimentacaoFinanceira m = new MovimentacaoFinanceira(TipoMovimentacao.SAIDA, f.getValor(), f.getData(),
				f.getDestino().trim(), OrigemMovimentacao.OUTRO_MEIO, "Despesa: " + f.getTipo().trim(), null, u);
		movimentos.saveAndFlush(m);
		Despesa d = new Despesa(f.getTipo().trim(), f.getDescricao().trim(), f.getNumeroNotaFiscal().trim(), m, u);
		despesas.saveAndFlush(d);
		return d.getId();
	}

	private Usuario responsavel() {
		return usuarios.findById(atual.exigirId()).orElseThrow();
	}

	private void validarPeriodo(LocalDate inicio, LocalDate fim) {
		if (inicio != null && fim != null && inicio.isAfter(fim))
			throw new OperacaoInvalidaException("PERIODO_INVALIDO", "A data inicial nao pode ser posterior a data final.");
	}

	private MovimentacaoDto dto(MovimentacaoFinanceira m) {
		return new MovimentacaoDto(m.getId(), m.getTipo(), m.getValor(), m.getDataMovimentacao(), m.getDestino(),
				m.getOrigem(), m.getOrigemDescricao(), m.getDoacao() == null ? null : m.getDoacao().getId());
	}

	private DespesaDto dto(Despesa d) {
		var m = d.getMovimentacaoFinanceira();
		return new DespesaDto(d.getId(), d.getTipo(), d.getDescricao(), d.getNumeroNotaFiscal(), m.getValor(),
				m.getDataMovimentacao(), m.getDestino());
	}

}
