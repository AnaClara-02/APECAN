package com.aclg.apecan.doacao.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "doacoes")
public class Doacao extends EntidadeCriada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_doacao")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_doacao", nullable = false, length = 30)
    private TipoDoacao tipo;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantidade;

    @Column(length = 30)
    private String unidade;

    @Column(name = "data_doacao", nullable = false)
    private LocalDate dataDoacao;

    @Column(name = "fonte_doacao", nullable = false, length = 150)
    private String fonteDoacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_usuario_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_doacoes_registrado_por"))
    private Usuario registradoPor;

    protected Doacao() {
    }

    private Doacao(TipoDoacao tipo, BigDecimal quantidade, String unidade,
                   LocalDate dataDoacao, String fonteDoacao,
                   Usuario registradoPor) {
        this.tipo = Objects.requireNonNull(tipo);
        this.quantidade = quantidade;
        this.unidade = unidade;
        this.dataDoacao = Objects.requireNonNull(dataDoacao);
        this.fonteDoacao = Objects.requireNonNull(fonteDoacao);
        this.registradoPor = Objects.requireNonNull(registradoPor);
        validarConteudo();
    }

    public static Doacao monetaria(LocalDate dataDoacao, String fonteDoacao,
                                   Usuario registradoPor) {
        return new Doacao(TipoDoacao.MONETARIA, null, null,
            dataDoacao, fonteDoacao, registradoPor);
    }

    public static Doacao equipamento(LocalDate dataDoacao, String fonteDoacao,
                                     Usuario registradoPor) {
        return new Doacao(TipoDoacao.EQUIPAMENTO, null, null,
            dataDoacao, fonteDoacao, registradoPor);
    }

    public static Doacao outroBem(BigDecimal quantidade, String unidade,
                                  LocalDate dataDoacao, String fonteDoacao,
                                  Usuario registradoPor) {
        return new Doacao(TipoDoacao.OUTRO_BEM, quantidade, unidade,
            dataDoacao, fonteDoacao, registradoPor);
    }

    private void validarConteudo() {
        if (tipo == TipoDoacao.OUTRO_BEM) {
            if (quantidade == null || quantidade.signum() <= 0 || unidade == null) {
                throw new OperacaoInvalidaException(
                    "DOACAO_CONTEUDO_INVALIDO",
                    "Doação de outro bem exige quantidade positiva e unidade."
                );
            }
        } else if (quantidade != null || unidade != null) {
            throw new OperacaoInvalidaException(
                "DOACAO_CONTEUDO_INVALIDO",
                "Doação monetária ou de equipamento não utiliza quantidade e unidade."
            );
        }
    }

    @PrePersist
    private void validarAntesDePersistir() {
        validarConteudo();
    }

    public Long getId() { return id; }
    public TipoDoacao getTipo() { return tipo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public String getUnidade() { return unidade; }
    public LocalDate getDataDoacao() { return dataDoacao; }
    public String getFonteDoacao() { return fonteDoacao; }
    public Usuario getRegistradoPor() { return registradoPor; }
}
