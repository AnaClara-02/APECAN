package com.aclg.apecan.relatorio.service;

import com.aclg.apecan.despesa.repository.DespesaRepository;
import com.aclg.apecan.doacao.entity.TipoDoacao;
import com.aclg.apecan.doacao.repository.DoacaoRepository;
import com.aclg.apecan.emprestimo.repository.EmprestimoEquipamentoRepository;
import com.aclg.apecan.equipamento.dto.ResumoCategoriaEquipamento;
import com.aclg.apecan.equipamento.repository.EquipamentoRepository;
import com.aclg.apecan.financeiro.entity.TipoMovimentacao;
import com.aclg.apecan.financeiro.repository.MovimentacaoFinanceiraRepository;
import com.aclg.apecan.paciente.entity.StatusPaciente;
import com.aclg.apecan.paciente.repository.PacienteRepository;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelatorioService {
    private final PacienteRepository pacientes;
    private final EquipamentoRepository equipamentos;
    private final EmprestimoEquipamentoRepository emprestimos;
    private final DoacaoRepository doacoes;
    private final MovimentacaoFinanceiraRepository movimentos;
    private final DespesaRepository despesas;
    private final Clock clock;

    public RelatorioService(PacienteRepository pacientes, EquipamentoRepository equipamentos,
                            EmprestimoEquipamentoRepository emprestimos, DoacaoRepository doacoes,
                            MovimentacaoFinanceiraRepository movimentos, DespesaRepository despesas, Clock clock) {
        this.pacientes = pacientes;
        this.equipamentos = equipamentos;
        this.emprestimos = emprestimos;
        this.doacoes = doacoes;
        this.movimentos = movimentos;
        this.despesas = despesas;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Resultado gerar(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new OperacaoInvalidaException("PERIODO_INVALIDO",
                    "A data inicial não pode ser posterior à data final.");
        }
        Map<StatusPaciente, Long> totaisPacientes = new EnumMap<>(StatusPaciente.class);
        for (StatusPaciente status : StatusPaciente.values()) {
            totaisPacientes.put(status, pacientes.countByStatus(status));
        }
        Map<TipoDoacao, Long> totaisDoacoes = new EnumMap<>(TipoDoacao.class);
        for (TipoDoacao tipo : TipoDoacao.values()) {
            totaisDoacoes.put(tipo, doacoes.contar(tipo, inicio, fim));
        }
        BigDecimal entradas = movimentos.total(TipoMovimentacao.ENTRADA, inicio, fim);
        BigDecimal saidas = movimentos.total(TipoMovimentacao.SAIDA, inicio, fim);
        return new Resultado(totaisPacientes, equipamentos.resumirPorCategoria(),
                emprestimos.countByDataDevolucaoIsNull(), emprestimos.countAtrasados(LocalDate.now(clock)),
                emprestimos.countByDataDevolucaoIsNotNull(), totaisDoacoes, entradas, saidas,
                entradas.subtract(saidas), despesas.contarNoPeriodo(inicio, fim),
                despesas.somarNoPeriodo(inicio, fim));
    }

    @Transactional(readOnly = true)
    public ResumoInicio resumoInicio() {
        BigDecimal entradas = movimentos.total(TipoMovimentacao.ENTRADA, null, null);
        BigDecimal saidas = movimentos.total(TipoMovimentacao.SAIDA, null, null);
        return new ResumoInicio(pacientes.countByStatus(StatusPaciente.ATIVO),
                emprestimos.countByDataDevolucaoIsNull(), emprestimos.countAtrasados(LocalDate.now(clock)),
                entradas.subtract(saidas));
    }

    public record ResumoInicio(long pacientesAtivos, long emprestimosAbertos,
                                long emprestimosAtrasados, BigDecimal saldo) {
    }

    public record Resultado(Map<StatusPaciente, Long> pacientes, List<ResumoCategoriaEquipamento> equipamentos,
                            long emprestimosAbertos, long emprestimosAtrasados, long emprestimosConcluidos,
                            Map<TipoDoacao, Long> doacoes, BigDecimal entradas, BigDecimal saidas, BigDecimal saldo,
                            long quantidadeDespesas, BigDecimal totalDespesas) {
        public long pacientesAtivos() {
            return pacientes.getOrDefault(StatusPaciente.ATIVO, 0L);
        }
    }
}
