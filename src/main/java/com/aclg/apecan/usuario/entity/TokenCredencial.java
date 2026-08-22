package com.aclg.apecan.usuario.entity;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "tokens_credencial",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tokens_credencial_hash",
        columnNames = "token_hash"
    )
)
public class TokenCredencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "id_usuario",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tokens_credencial_usuario")
    )
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FinalidadeTokenCredencial finalidade;

    @Column(name = "solicitado_em", nullable = false, updatable = false)
    private LocalDateTime solicitadoEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "utilizado_em")
    private LocalDateTime utilizadoEm;

    protected TokenCredencial() {
    }

    public TokenCredencial(Usuario usuario, String tokenHash,
                           FinalidadeTokenCredencial finalidade,
                           LocalDateTime solicitadoEm,
                           LocalDateTime expiraEm) {
        this.usuario = Objects.requireNonNull(usuario);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.finalidade = Objects.requireNonNull(finalidade);
        this.solicitadoEm = Objects.requireNonNull(solicitadoEm);
        this.expiraEm = Objects.requireNonNull(expiraEm);
        if (!expiraEm.isAfter(solicitadoEm)) {
            throw new OperacaoInvalidaException(
                "EXPIRACAO_TOKEN_INVALIDA",
                "A expiracao deve ser posterior a solicitacao."
            );
        }
    }

    public boolean estaExpirado(LocalDateTime instante) {
        return !expiraEm.isAfter(instante);
    }

    public boolean foiUtilizado() {
        return utilizadoEm != null;
    }

    public void marcarComoUtilizado(LocalDateTime instante) {
        if (foiUtilizado()) {
            throw new OperacaoInvalidaException(
                "TOKEN_JA_UTILIZADO",
                "O token ja foi utilizado."
            );
        }
        utilizadoEm = Objects.requireNonNull(instante);
    }

    public void invalidar(LocalDateTime instante) {
        if (!foiUtilizado()) {
            utilizadoEm = Objects.requireNonNull(instante);
        }
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public FinalidadeTokenCredencial getFinalidade() { return finalidade; }
    public LocalDateTime getSolicitadoEm() { return solicitadoEm; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public LocalDateTime getUtilizadoEm() { return utilizadoEm; }
}
