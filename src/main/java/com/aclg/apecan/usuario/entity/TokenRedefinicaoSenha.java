package com.aclg.apecan.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "tokens_redefinicao_senha",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tokens_redefinicao_senha_hash",
        columnNames = "token_hash"
    )
)
public class TokenRedefinicaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "id_usuario",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tokens_redefinicao_usuario")
    )
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "solicitado_em", nullable = false, updatable = false)
    private LocalDateTime solicitadoEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "utilizado_em")
    private LocalDateTime utilizadoEm;

    protected TokenRedefinicaoSenha() {
    }

    public TokenRedefinicaoSenha(Usuario usuario, String tokenHash,
                                 LocalDateTime expiraEm) {
        this.usuario = Objects.requireNonNull(usuario);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiraEm = Objects.requireNonNull(expiraEm);
    }

    @PrePersist
    private void aoCriar() {
        if (solicitadoEm == null) {
            solicitadoEm = LocalDateTime.now();
        }
        if (!expiraEm.isAfter(solicitadoEm)) {
            throw new IllegalStateException("A expiração deve ser posterior à solicitação.");
        }
    }

    public boolean estaExpirado(LocalDateTime instante) {
        return !expiraEm.isAfter(instante);
    }

    public boolean foiUtilizado() {
        return utilizadoEm != null;
    }

    public void marcarComoUtilizado() {
        if (foiUtilizado()) {
            throw new IllegalStateException("O token já foi utilizado.");
        }
        utilizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getSolicitadoEm() { return solicitadoEm; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public LocalDateTime getUtilizadoEm() { return utilizadoEm; }
}
