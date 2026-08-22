package com.aclg.apecan.despesa.entity;

import com.aclg.apecan.financeiro.entity.MovimentacaoFinanceira;
import com.aclg.apecan.financeiro.entity.TipoMovimentacao;
import com.aclg.apecan.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "despesas",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_despesas_movimentacao",
        columnNames = "id_movimentacao_financeira"
    )
)
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_despesa")
    private Long id;

    @Column(name = "tipo_despesa", nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(name = "numero_nota_fiscal", nullable = false, length = 20)
    private String numeroNotaFiscal;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_movimentacao_financeira", nullable = false,
        foreignKey = @ForeignKey(name = "fk_despesas_movimentacao"))
    private MovimentacaoFinanceira movimentacaoFinanceira;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_usuario_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_despesas_registrado_por"))
    private Usuario registradoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Despesa() {
    }

    public Despesa(String tipo, String descricao, String numeroNotaFiscal,
                   MovimentacaoFinanceira movimentacaoFinanceira,
                   Usuario registradoPor) {
        this.tipo = Objects.requireNonNull(tipo);
        this.descricao = Objects.requireNonNull(descricao);
        this.numeroNotaFiscal = Objects.requireNonNull(numeroNotaFiscal);
        this.movimentacaoFinanceira = Objects.requireNonNull(movimentacaoFinanceira);
        this.registradoPor = Objects.requireNonNull(registradoPor);
        validarMovimentacao();
    }

    private void validarMovimentacao() {
        if (movimentacaoFinanceira.getTipo() != TipoMovimentacao.SAIDA) {
            throw new IllegalArgumentException(
                "Uma despesa deve estar vinculada a uma movimentação de SAÍDA."
            );
        }
    }

    @PrePersist
    private void aoCriar() {
        validarMovimentacao();
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getDescricao() { return descricao; }
    public String getNumeroNotaFiscal() { return numeroNotaFiscal; }
    public MovimentacaoFinanceira getMovimentacaoFinanceira() {
        return movimentacaoFinanceira;
    }
    public Usuario getRegistradoPor() { return registradoPor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
