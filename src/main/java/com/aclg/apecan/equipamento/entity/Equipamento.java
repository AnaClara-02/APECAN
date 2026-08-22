package com.aclg.apecan.equipamento.entity;

import com.aclg.apecan.doacao.entity.Doacao;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "equipamentos")
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipamento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false,
        foreignKey = @ForeignKey(name = "fk_equipamentos_categoria"))
    private CategoriaEquipamento categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_doacao",
        foreignKey = @ForeignKey(name = "fk_equipamentos_doacao"))
    private Doacao doacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_conservacao", nullable = false, length = 15)
    private EstadoConservacao estadoConservacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusEquipamento status = StatusEquipamento.ATIVO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_usuario_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_equipamentos_criado_por"))
    private Usuario criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    protected Equipamento() {
    }

    public Equipamento(CategoriaEquipamento categoria, Doacao doacao,
                       EstadoConservacao estadoConservacao, Usuario criadoPor) {
        this.categoria = Objects.requireNonNull(categoria);
        this.doacao = doacao;
        this.estadoConservacao = Objects.requireNonNull(estadoConservacao);
        this.criadoPor = Objects.requireNonNull(criadoPor);
    }

    public void atualizarCategoria(CategoriaEquipamento categoria) {
        this.categoria = Objects.requireNonNull(categoria);
    }

    public void atualizarEstadoConservacao(EstadoConservacao estado) {
        this.estadoConservacao = Objects.requireNonNull(estado);
    }

    public void emprestar() {
        if (status != StatusEquipamento.ATIVO) {
            throw new IllegalStateException("Somente equipamento ativo pode ser emprestado.");
        }
        status = StatusEquipamento.EMPRESTADO;
    }

    public void registrarDevolucao() {
        if (status != StatusEquipamento.EMPRESTADO) {
            throw new IllegalStateException("O equipamento não está emprestado.");
        }
        status = StatusEquipamento.ATIVO;
    }

    public void desativar() {
        if (status == StatusEquipamento.EMPRESTADO) {
            throw new IllegalStateException("Equipamento emprestado não pode ser desativado.");
        }
        status = StatusEquipamento.INATIVO;
    }

    public void reativar() {
        if (status == StatusEquipamento.EMPRESTADO) {
            throw new IllegalStateException("Equipamento emprestado não pode ser reativado.");
        }
        status = StatusEquipamento.ATIVO;
    }

    @PrePersist
    private void aoCriar() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }

    @PreUpdate
    private void aoAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public CategoriaEquipamento getCategoria() { return categoria; }
    public Doacao getDoacao() { return doacao; }
    public EstadoConservacao getEstadoConservacao() { return estadoConservacao; }
    public StatusEquipamento getStatus() { return status; }
    public Usuario getCriadoPor() { return criadoPor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
