package com.aclg.apecan.despesa.entity;

import com.aclg.apecan.financeiro.entity.MovimentacaoFinanceira;
import com.aclg.apecan.financeiro.entity.TipoMovimentacao;
import com.aclg.apecan.shared.audit.EntidadeCriada;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.shared.validation.CnpjValidator;
import com.aclg.apecan.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "despesas", uniqueConstraints = @UniqueConstraint(
		name = "uq_despesas_movimentacao", columnNames = "id_movimentacao_financeira"))
public class Despesa extends EntidadeCriada {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_despesa")
	private Long id;

	@Column(name = "tipo_despesa", nullable = false, length = 50)
	private String tipo;

	@Column(nullable = false, length = 255)
	private String descricao;

	@Column(name = "nome_emitente", length = 150)
	private String nomeEmitente;

	@Column(name = "cnpj_emitente", length = 14)
	private String cnpjEmitente;

	@Column(name = "numero_nota_fiscal", length = 20)
	private String numeroNotaFiscal;

	@Column(name = "data_emissao")
	private LocalDate dataEmissao;

	@Enumerated(EnumType.STRING)
	@Column(name = "banco_pagamento", length = 10)
	private BancoPagamentoDespesa bancoPagamento;

	@Column(name = "outro_banco_pagamento", length = 150)
	private String outroBancoPagamento;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_movimentacao_financeira", nullable = false,
		foreignKey = @ForeignKey(name = "fk_despesas_movimentacao"))
	private MovimentacaoFinanceira movimentacaoFinanceira;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "registrado_por_usuario_id", nullable = false,
		foreignKey = @ForeignKey(name = "fk_despesas_registrado_por"))
	private Usuario registradoPor;

	protected Despesa() {
	}

	public Despesa(String tipo, String descricao, String nomeEmitente, String cnpjEmitente,
			String numeroNotaFiscal, LocalDate dataEmissao, BancoPagamentoDespesa bancoPagamento,
			String outroBancoPagamento, MovimentacaoFinanceira movimentacaoFinanceira, Usuario registradoPor) {
		this.tipo = Objects.requireNonNull(tipo);
		this.descricao = Objects.requireNonNull(descricao);
		this.nomeEmitente = nomeEmitente;
		this.cnpjEmitente = cnpjEmitente;
		this.numeroNotaFiscal = numeroNotaFiscal;
		this.dataEmissao = dataEmissao;
		this.bancoPagamento = Objects.requireNonNull(bancoPagamento);
		this.outroBancoPagamento = outroBancoPagamento;
		this.movimentacaoFinanceira = Objects.requireNonNull(movimentacaoFinanceira);
		this.registradoPor = Objects.requireNonNull(registradoPor);
		validarDespesa();
	}

	private void validarDespesa() {
		if (movimentacaoFinanceira.getTipo() != TipoMovimentacao.SAIDA) {
			throw new OperacaoInvalidaException("DESPESA_MOVIMENTACAO_INVALIDA",
					"Uma despesa deve estar vinculada a uma movimentação de SAÍDA.");
		}
		boolean temNota = numeroNotaFiscal != null && !numeroNotaFiscal.isBlank();
		boolean temDadosFiscais = nomeEmitente != null || cnpjEmitente != null || dataEmissao != null;
		if (temNota != temDadosFiscais || (temNota && (nomeEmitente == null || nomeEmitente.isBlank()
				|| cnpjEmitente == null || !CnpjValidator.valido(cnpjEmitente) || dataEmissao == null))) {
			throw new OperacaoInvalidaException("DADOS_NOTA_FISCAL_INVALIDOS",
					"Os dados da nota fiscal estão incompletos ou inválidos.");
		}
		if (bancoPagamento == BancoPagamentoDespesa.OUTRO
				&& (outroBancoPagamento == null || outroBancoPagamento.isBlank())) {
			throw new OperacaoInvalidaException("BANCO_PAGAMENTO_INVALIDO",
					"Informe o nome do banco de pagamento selecionado como Outro.");
		}
		if (bancoPagamento != BancoPagamentoDespesa.OUTRO && outroBancoPagamento != null) {
			throw new OperacaoInvalidaException("BANCO_PAGAMENTO_INVALIDO",
					"O banco alternativo só pode ser preenchido quando Outro estiver selecionado.");
		}
	}

	@PrePersist
	private void validarAntesDePersistir() {
		validarDespesa();
	}

	public Long getId() { return id; }
	public String getTipo() { return tipo; }
	public String getDescricao() { return descricao; }
	public String getNomeEmitente() { return nomeEmitente; }
	public String getCnpjEmitente() { return cnpjEmitente; }
	public String getNumeroNotaFiscal() { return numeroNotaFiscal; }
	public LocalDate getDataEmissao() { return dataEmissao; }
	public BancoPagamentoDespesa getBancoPagamento() { return bancoPagamento; }
	public String getOutroBancoPagamento() { return outroBancoPagamento; }
	public MovimentacaoFinanceira getMovimentacaoFinanceira() { return movimentacaoFinanceira; }
	public Usuario getRegistradoPor() { return registradoPor; }
}
