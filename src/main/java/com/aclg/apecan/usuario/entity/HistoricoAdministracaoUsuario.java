package com.aclg.apecan.usuario.entity;

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

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "historico_administracao_usuarios")
public class HistoricoAdministracaoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "id_usuario",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_historico_administracao_usuario")
    )
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "realizado_por_usuario_id",
        foreignKey = @ForeignKey(name = "fk_historico_administracao_responsavel")
    )
    private Usuario realizadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEventoAdministracaoUsuario tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_anterior", length = 20)
    private TipoPerfil perfilAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_novo", length = 20)
    private TipoPerfil perfilNovo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 10)
    private StatusUsuario statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", length = 10)
    private StatusUsuario statusNovo;

    @Column(length = 500)
    private String justificativa;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private LocalDateTime ocorridoEm;

    protected HistoricoAdministracaoUsuario() {
    }

    public HistoricoAdministracaoUsuario(
            Usuario usuario,
            Usuario realizadoPor,
            TipoEventoAdministracaoUsuario tipoEvento,
            TipoPerfil perfilAnterior,
            TipoPerfil perfilNovo,
            StatusUsuario statusAnterior,
            StatusUsuario statusNovo,
            String justificativa,
            LocalDateTime ocorridoEm) {
        this.usuario = Objects.requireNonNull(usuario);
        this.realizadoPor = realizadoPor;
        this.tipoEvento = Objects.requireNonNull(tipoEvento);
        this.perfilAnterior = perfilAnterior;
        this.perfilNovo = perfilNovo;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.justificativa = justificativa;
        this.ocorridoEm = Objects.requireNonNull(ocorridoEm);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Usuario getRealizadoPor() { return realizadoPor; }
    public TipoEventoAdministracaoUsuario getTipoEvento() { return tipoEvento; }
    public TipoPerfil getPerfilAnterior() { return perfilAnterior; }
    public TipoPerfil getPerfilNovo() { return perfilNovo; }
    public StatusUsuario getStatusAnterior() { return statusAnterior; }
    public StatusUsuario getStatusNovo() { return statusNovo; }
    public String getJustificativa() { return justificativa; }
    public LocalDateTime getOcorridoEm() { return ocorridoEm; }
}
