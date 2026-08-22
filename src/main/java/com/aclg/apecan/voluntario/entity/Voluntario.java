package com.aclg.apecan.voluntario.entity;

import com.aclg.apecan.shared.audit.EntidadeAuditavel;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.shared.validation.CpfNormalizer;
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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "voluntarios",
    uniqueConstraints = @UniqueConstraint(name = "uq_voluntarios_cpf", columnNames = "cpf")
)
public class Voluntario extends EntidadeAuditavel {

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

    @Column(name = "desativado_em")
    private LocalDateTime desativadoEm;

    protected Voluntario() {
    }

    public Voluntario(String nome, String cpf, LocalDate dataNascimento,
                      String telefone, String endereco, Usuario criadoPor,
                      LocalDate hoje) {
        this.nome = Objects.requireNonNull(nome);
        this.cpf = CpfNormalizer.normalizar(cpf);
        this.dataNascimento = Objects.requireNonNull(dataNascimento);
        this.telefone = Objects.requireNonNull(telefone);
        this.endereco = Objects.requireNonNull(endereco);
        this.criadoPor = Objects.requireNonNull(criadoPor);
        validarDataNascimento(hoje);
    }

    public void atualizarDados(String nome, LocalDate dataNascimento,
                               String telefone, String endereco,
                               Usuario responsavel, LocalDate hoje) {
        this.nome = Objects.requireNonNull(nome);
        this.dataNascimento = Objects.requireNonNull(dataNascimento);
        this.telefone = Objects.requireNonNull(telefone);
        this.endereco = Objects.requireNonNull(endereco);
        this.atualizadoPor = Objects.requireNonNull(responsavel);
        validarDataNascimento(hoje);
    }

    public void desativar(Usuario responsavel, LocalDateTime instante) {
        this.status = StatusVoluntario.INATIVO;
        this.desativadoEm = Objects.requireNonNull(instante);
        this.atualizadoPor = Objects.requireNonNull(responsavel);
    }

    public void reativar(Usuario responsavel) {
        this.status = StatusVoluntario.ATIVO;
        this.desativadoEm = null;
        this.atualizadoPor = Objects.requireNonNull(responsavel);
    }

    private void validarDataNascimento(LocalDate hoje) {
        if (dataNascimento.isAfter(Objects.requireNonNull(hoje))) {
            throw new OperacaoInvalidaException(
                "DATA_NASCIMENTO_FUTURA",
                "A data de nascimento não pode estar no futuro."
            );
        }
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
    public LocalDateTime getDesativadoEm() { return desativadoEm; }
}
