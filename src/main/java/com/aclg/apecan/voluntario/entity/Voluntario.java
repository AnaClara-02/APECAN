package com.aclg.apecan.voluntario.entity;

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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "voluntarios",
    uniqueConstraints = @UniqueConstraint(name = "uq_voluntarios_cpf", columnNames = "cpf")
)
public class Voluntario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_voluntario")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false, length = 15)
    private String telefone;

    @Column(nullable = false, length = 255)
    private String endereco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusVoluntario status = StatusVoluntario.ATIVO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_usuario_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_voluntarios_criado_por"))
    private Usuario criadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por_usuario_id",
        foreignKey = @ForeignKey(name = "fk_voluntarios_atualizado_por"))
    private Usuario atualizadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "desativado_em")
    private LocalDateTime desativadoEm;

    protected Voluntario() {
    }

    public Voluntario(String nome, String cpf, LocalDate dataNascimento,
                      String telefone, String endereco, Usuario criadoPor) {
        this.nome = Objects.requireNonNull(nome);
        this.cpf = Objects.requireNonNull(cpf);
        this.dataNascimento = Objects.requireNonNull(dataNascimento);
        this.telefone = Objects.requireNonNull(telefone);
        this.endereco = Objects.requireNonNull(endereco);
        this.criadoPor = Objects.requireNonNull(criadoPor);
    }

    public void atualizarDados(String nome, LocalDate dataNascimento,
                               String telefone, String endereco,
                               Usuario responsavel) {
        this.nome = Objects.requireNonNull(nome);
        this.dataNascimento = Objects.requireNonNull(dataNascimento);
        this.telefone = Objects.requireNonNull(telefone);
        this.endereco = Objects.requireNonNull(endereco);
        this.atualizadoPor = Objects.requireNonNull(responsavel);
    }

    public void desativar(Usuario responsavel) {
        this.status = StatusVoluntario.INATIVO;
        this.desativadoEm = LocalDateTime.now();
        this.atualizadoPor = Objects.requireNonNull(responsavel);
    }

    public void reativar(Usuario responsavel) {
        this.status = StatusVoluntario.ATIVO;
        this.desativadoEm = null;
        this.atualizadoPor = Objects.requireNonNull(responsavel);
    }

    @PrePersist
    private void aoCriar() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
        if (dataNascimento.isAfter(LocalDate.now())) {
            throw new IllegalStateException("A data de nascimento não pode estar no futuro.");
        }
    }

    @PreUpdate
    private void aoAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public String getTelefone() { return telefone; }
    public String getEndereco() { return endereco; }
    public StatusVoluntario getStatus() { return status; }
    public Usuario getCriadoPor() { return criadoPor; }
    public Usuario getAtualizadoPor() { return atualizadoPor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public LocalDateTime getDesativadoEm() { return desativadoEm; }
}
