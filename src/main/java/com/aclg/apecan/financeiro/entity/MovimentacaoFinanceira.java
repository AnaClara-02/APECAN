package com.aclg.apecan.financeiro.entity;

import com.aclg.apecan.doacao.entity.Doacao;
import com.aclg.apecan.doacao.entity.TipoDoacao;
import com.aclg.apecan.shared.audit.EntidadeCriada;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "movimentacoes_financeiras",
		uniqueConstraints = @UniqueConstraint(name = "uq_movimentacoes_doacao", columnNames = "id_doacao"))
public class MovimentacaoFinanceira extends EntidadeCriada {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_movimentacao")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_movimentacao", nullable = false, length = 10)
	private TipoMovimentacao tipo;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal valor;

	@Column(name = "data_movimentacao", nullable = false)
	private LocalDate dataMovimentacao;

	@Column(nullable = false, length = 150)
	private String destino;

	@Enumerated(EnumType.STRING)
	@Column(name = "origem_tipo", nullable = false, length = 20)
	private OrigemMovimentacao origem;

	@Column(name = "origem_descricao", nullable = false, length = 150)
	private String origemDescricao;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_doacao", foreignKey = @ForeignKey(name = "fk_movimentacoes_doacao"))
	private Doacao doacao;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "registrado_por_usuario_id", nullable = false,
			foreignKey = @ForeignKey(name = "fk_movimentacoes_registrado_por"))
	private Usuario registradoPor;

	protected MovimentacaoFinanceira() {
	}

	public MovimentacaoFinanceira(TipoMovimentacao tipo, BigDecimal valor, LocalDate dataMovimentacao, String destino,
			OrigemMovimentacao origem, String origemDescricao, Doacao doacao, Usuario registradoPor) {
		this.tipo = Objects.requireNonNull(tipo);
		this.valor = Objects.requireNonNull(valor);
		this.dataMovimentacao = Objects.requireNonNull(dataMovimentacao);
		this.destino = Objects.requireNonNull(destino);
		this.origem = Objects.requireNonNull(origem);
		this.origemDescricao = Objects.requireNonNull(origemDescricao);
		this.doacao = doacao;
		this.registradoPor = Objects.requireNonNull(registradoPor);
		validar();
	}

	private void validar() {
		if (valor.signum() <= 0) {
			throw new OperacaoInvalidaException("MOVIMENTACAO_VALOR_INVALIDO",
					"O valor da movimentação deve ser positivo.");
		}
		if (origem == OrigemMovimentacao.DOACAO && (doacao == null || tipo != TipoMovimentacao.ENTRADA)) {
			throw new OperacaoInvalidaException("MOVIMENTACAO_DOACAO_INVALIDA",
					"Movimentação originada por doação exige doação e tipo ENTRADA.");
		}
		if (doacao != null && doacao.getTipo() != TipoDoacao.MONETARIA) {
			throw new OperacaoInvalidaException("MOVIMENTACAO_DOACAO_NAO_MONETARIA",
					"Somente doacao monetaria pode gerar movimentacao financeira.");
		}
		if (origem == OrigemMovimentacao.OUTRO_MEIO && doacao != null) {
			throw new OperacaoInvalidaException("MOVIMENTACAO_ORIGEM_INVALIDA",
					"Movimentação de outro meio não pode estar ligada a uma doação.");
		}
	}

	@PrePersist
	private void validarAntesDePersistir() {
		validar();
	}

	public Long getId() {
		return id;
	}

	public TipoMovimentacao getTipo() {
		return tipo;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public LocalDate getDataMovimentacao() {
		return dataMovimentacao;
	}

	public String getDestino() {
		return destino;
	}

	public OrigemMovimentacao getOrigem() {
		return origem;
	}

	public String getOrigemDescricao() {
		return origemDescricao;
	}

	public Doacao getDoacao() {
		return doacao;
	}

	public Usuario getRegistradoPor() {
		return registradoPor;
	}

}
