package com.aclg.apecan.relatorio.service;

import com.aclg.apecan.doacao.entity.TipoDoacao;
import com.aclg.apecan.doacao.repository.DoacaoRepository;
import com.aclg.apecan.emprestimo.repository.EmprestimoEquipamentoRepository;
import com.aclg.apecan.equipamento.dto.ResumoCategoriaEquipamento;
import com.aclg.apecan.equipamento.repository.EquipamentoRepository;
import com.aclg.apecan.financeiro.entity.TipoMovimentacao;
import com.aclg.apecan.financeiro.repository.MovimentacaoFinanceiraRepository;
import com.aclg.apecan.paciente.entity.StatusPaciente;
import com.aclg.apecan.paciente.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class RelatorioService {

	private final PacienteRepository pacientes;

	private final EquipamentoRepository equipamentos;

	private final EmprestimoEquipamentoRepository emprestimos;

	private final DoacaoRepository doacoes;

	private final MovimentacaoFinanceiraRepository movimentos;

	private final Clock clock;

	public RelatorioService(PacienteRepository p, EquipamentoRepository e, EmprestimoEquipamentoRepository em,
			DoacaoRepository d, MovimentacaoFinanceiraRepository m, Clock c) {
		pacientes = p;
		equipamentos = e;
		emprestimos = em;
		doacoes = d;
		movimentos = m;
		clock = c;
	}

	@Transactional(readOnly = true)
	public Resultado gerar(LocalDate inicio, LocalDate fim) {
		Map<StatusPaciente, Long> ps = new EnumMap<>(StatusPaciente.class);
		for (var s : StatusPaciente.values())
			ps.put(s, pacientes.countByStatus(s));
		Map<TipoDoacao, Long> ds = new EnumMap<>(TipoDoacao.class);
		for (var t : TipoDoacao.values())
			ds.put(t, doacoes.contar(t, inicio, fim));
		BigDecimal entradas = movimentos.total(TipoMovimentacao.ENTRADA, inicio, fim),
				saidas = movimentos.total(TipoMovimentacao.SAIDA, inicio, fim);
		return new Resultado(ps, equipamentos.resumirPorCategoria(), emprestimos.countByDataDevolucaoIsNull(),
				emprestimos.countAtrasados(LocalDate.now(clock)), emprestimos.countByDataDevolucaoIsNotNull(), ds,
				entradas, saidas, entradas.subtract(saidas));
	}

	public record Resultado(Map<StatusPaciente, Long> pacientes, List<ResumoCategoriaEquipamento> equipamentos,
			long emprestimosAbertos, long emprestimosAtrasados, long emprestimosConcluidos,
			Map<TipoDoacao, Long> doacoes, BigDecimal entradas, BigDecimal saidas, BigDecimal saldo) {
	}

}
