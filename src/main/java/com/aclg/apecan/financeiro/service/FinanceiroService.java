package com.aclg.apecan.financeiro.service;

import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.despesa.dto.DespesaDto;
import com.aclg.apecan.despesa.dto.DespesaForm;
import com.aclg.apecan.despesa.entity.BancoPagamentoDespesa;
import com.aclg.apecan.despesa.entity.Despesa;
import com.aclg.apecan.despesa.repository.DespesaRepository;
import com.aclg.apecan.doacao.entity.TipoDoacao;
import com.aclg.apecan.financeiro.dto.MovimentacaoDto;
import com.aclg.apecan.financeiro.dto.MovimentacaoForm;
import com.aclg.apecan.financeiro.entity.MovimentacaoFinanceira;
import com.aclg.apecan.financeiro.entity.OrigemMovimentacao;
import com.aclg.apecan.financeiro.entity.TipoMovimentacao;
import com.aclg.apecan.financeiro.repository.MovimentacaoFinanceiraRepository;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.shared.exception.RecursoNaoEncontradoException;
import com.aclg.apecan.shared.validation.CnpjValidator;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class FinanceiroService {
    private final MovimentacaoFinanceiraRepository movimentos;
    private final DespesaRepository despesas;
    private final UsuarioRepository usuarios;
    private final UsuarioAtual atual;
    private final Clock clock;

    public FinanceiroService(MovimentacaoFinanceiraRepository movimentos, DespesaRepository despesas,
                             UsuarioRepository usuarios, UsuarioAtual atual, Clock clock) {
        this.movimentos = movimentos;
        this.despesas = despesas;
        this.usuarios = usuarios;
        this.atual = atual;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoDto> listar(TipoMovimentacao tipo, LocalDate inicio, LocalDate fim, int pagina) {
        validarPeriodo(inicio, fim);
        Pageable pageable = PageRequest.of(Math.max(0, pagina), 20, Sort.by("dataMovimentacao").descending());
        return movimentos.pesquisar(tipo, inicio, fim, pageable).map(this::dto);
    }

    @Transactional(readOnly = true)
    public BigDecimal saldo(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        return movimentos.saldo(inicio, fim);
    }

    @Transactional(readOnly = true)
    public Page<DespesaDto> despesas(LocalDate inicio, LocalDate fim, int pagina) {
        validarPeriodo(inicio, fim);
        return despesas.pesquisar(inicio, fim,
                PageRequest.of(Math.max(0, pagina), 20)).map(this::dto);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalDespesas(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        return despesas.somarNoPeriodo(inicio, fim);
    }

    @Transactional(readOnly = true)
    public DespesaDto buscarDespesa(Long id) {
        return despesas.buscarDetalhe(id).map(this::dto).orElseThrow(
                () -> new RecursoNaoEncontradoException("DESPESA_NAO_ENCONTRADA", "Despesa não encontrada."));
    }

    @Transactional
    public Long registrar(@Valid MovimentacaoForm formulario) {
        MovimentacaoFinanceira movimentacao = new MovimentacaoFinanceira(formulario.getTipo(),
                formulario.getValor(), formulario.getData(), formulario.getDestino().trim(),
                OrigemMovimentacao.OUTRO_MEIO, formulario.getDescricao().trim(), null, responsavel());
        movimentos.saveAndFlush(movimentacao);
        return movimentacao.getId();
    }

    @Transactional
    public Long registrarDespesa(@Valid DespesaForm formulario) {
        LocalDate hoje = LocalDate.now(clock);
        if (formulario.getDataPagamento().isAfter(hoje)) {
            throw new OperacaoInvalidaException("DATA_PAGAMENTO_FUTURA",
                    "A data de pagamento não pode ser futura.");
        }
        if (formulario.isPossuiNotaFiscal() && formulario.getDataEmissao().isAfter(hoje)) {
            throw new OperacaoInvalidaException("DATA_EMISSAO_FUTURA",
                    "A data de emissão não pode ser futura.");
        }

        String nomeEmitente = formulario.isPossuiNotaFiscal() ? formulario.getNomeEmitente().trim() : null;
        String cnpjEmitente = formulario.isPossuiNotaFiscal()
                ? CnpjValidator.normalizar(formulario.getCnpjEmitente()) : null;
        String numeroNota = formulario.isPossuiNotaFiscal() ? formulario.getNumeroNotaFiscal().trim() : null;
        LocalDate dataEmissao = formulario.isPossuiNotaFiscal() ? formulario.getDataEmissao() : null;
        BancoPagamentoDespesa banco = formulario.getBancoPagamento();
        String outroBanco = banco == BancoPagamentoDespesa.OUTRO
                ? formulario.getOutroBancoPagamento().trim() : null;

        Usuario usuario = responsavel();
        MovimentacaoFinanceira movimentacao = new MovimentacaoFinanceira(TipoMovimentacao.SAIDA,
                formulario.getValor(), formulario.getDataPagamento(), formulario.getDestino().trim(),
                OrigemMovimentacao.OUTRO_MEIO, "Despesa: " + formulario.getTipo().trim(), null, usuario);
        movimentos.saveAndFlush(movimentacao);
        Despesa despesa = new Despesa(formulario.getTipo().trim(), formulario.getDescricao().trim(),
                nomeEmitente, cnpjEmitente, numeroNota, dataEmissao, banco, outroBanco, movimentacao, usuario);
        despesas.saveAndFlush(despesa);
        return despesa.getId();
    }

    private Usuario responsavel() {
        return usuarios.findById(atual.exigirId()).orElseThrow();
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new OperacaoInvalidaException("PERIODO_INVALIDO",
                    "A data inicial não pode ser posterior à data final.");
        }
    }

    private MovimentacaoDto dto(MovimentacaoFinanceira movimento) {
        return new MovimentacaoDto(movimento.getId(), movimento.getTipo(), movimento.getValor(),
                movimento.getDataMovimentacao(), movimento.getDestino(), movimento.getOrigem(),
                movimento.getOrigemDescricao(), movimento.getDoacao() == null ? null : movimento.getDoacao().getId());
    }

    private DespesaDto dto(Despesa despesa) {
        MovimentacaoFinanceira movimento = despesa.getMovimentacaoFinanceira();
        return new DespesaDto(despesa.getId(), despesa.getTipo(), despesa.getDescricao(),
                despesa.getNomeEmitente(), despesa.getCnpjEmitente(), despesa.getNumeroNotaFiscal(),
                despesa.getDataEmissao(), despesa.getBancoPagamento(), despesa.getOutroBancoPagamento(),
                movimento.getValor(), movimento.getDataMovimentacao(), movimento.getDestino());
    }
}
