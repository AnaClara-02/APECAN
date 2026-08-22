package com.aclg.apecan.equipamento.entity;

import com.aclg.apecan.shared.audit.EntidadeAuditavel;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

@Entity
@Table(
    name = "categorias_equipamentos",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_categorias_equipamentos_nome",
        columnNames = "nome"
    )
)
public class CategoriaEquipamento extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_usuario_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_categorias_equipamentos_criado_por"))
    private Usuario criadoPor;

    protected CategoriaEquipamento() {
    }

    public CategoriaEquipamento(String nome, String descricao, Usuario criadoPor) {
        this.nome = Objects.requireNonNull(nome);
        this.descricao = descricao;
        this.criadoPor = Objects.requireNonNull(criadoPor);
    }

    public void atualizar(String nome, String descricao) {
        this.nome = Objects.requireNonNull(nome);
        this.descricao = descricao;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Usuario getCriadoPor() { return criadoPor; }
}
