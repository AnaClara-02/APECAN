package com.aclg.apecan.paciente.entity;

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
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "historico_status_paciente")
public class HistoricoStatusPaciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historico")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false,
        foreignKey = @ForeignKey(name = "fk_historico_status_paciente"))
    private Paciente paciente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusPaciente status;

    @Column(name = "alterado_em", nullable = false, updatable = false)
    private LocalDateTime alteradoEm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alterado_por_usuario_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_historico_status_usuario"))
    private Usuario alteradoPor;

    @Column(length = 255)
    private String observacao;

    protected HistoricoStatusPaciente() {
    }

    public HistoricoStatusPaciente(Paciente paciente, StatusPaciente status,
                                   LocalDateTime alteradoEm,
                                   Usuario alteradoPor, String observacao) {
        this.paciente = Objects.requireNonNull(paciente);
        this.status = Objects.requireNonNull(status);
        this.alteradoEm = Objects.requireNonNull(alteradoEm);
        this.alteradoPor = Objects.requireNonNull(alteradoPor);
        this.observacao = observacao;
    }

    public Long getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public StatusPaciente getStatus() { return status; }
    public LocalDateTime getAlteradoEm() { return alteradoEm; }
    public Usuario getAlteradoPor() { return alteradoPor; }
    public String getObservacao() { return observacao; }
}
