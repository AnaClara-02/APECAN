package com.aclg.apecan.usuario.entity;

import com.aclg.apecan.shared.audit.EntidadeAuditavel;
import com.aclg.apecan.shared.validation.CpfNormalizer;
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
    name = "usuarios",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_usuarios_login", columnNames = "login"),
        @UniqueConstraint(name = "uq_usuarios_cpf", columnNames = "cpf"),
        @UniqueConstraint(name = "uq_usuarios_email", columnNames = "email")
    }
)
public class Usuario extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(nullable = false, length = 50)
    private String login;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "foto_url", nullable = false, length = 500)
    private String fotoUrl;

    @Column(nullable = false, length = 15)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_de_perfil", nullable = false, length = 20)
    private TipoPerfil tipoPerfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusUsuario status = StatusUsuario.ATIVO;

    @Column(name = "primeiro_acesso_pendente", nullable = false)
    private boolean primeiroAcessoPendente = true;

    @Column(name = "senha_definitiva_em")
    private LocalDateTime senhaDefinitivaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "criado_por_usuario_id",
        foreignKey = @ForeignKey(name = "fk_usuarios_criado_por")
    )
    private Usuario criadoPor;

    @Column(name = "desativado_em")
    private LocalDateTime desativadoEm;

    protected Usuario() {
    }

    public Usuario(String nome, String senhaHash, String login, String cpf,
                   String email, String fotoUrl, String telefone,
                   TipoPerfil tipoPerfil, Usuario criadoPor) {
        this.nome = Objects.requireNonNull(nome);
        this.senhaHash = Objects.requireNonNull(senhaHash);
        this.login = Objects.requireNonNull(login);
        this.cpf = CpfNormalizer.normalizar(cpf);
        this.email = Objects.requireNonNull(email);
        this.fotoUrl = Objects.requireNonNull(fotoUrl);
        this.telefone = Objects.requireNonNull(telefone);
        this.tipoPerfil = Objects.requireNonNull(tipoPerfil);
        this.criadoPor = criadoPor;
    }

    public void atualizarDadosPessoais(String nome, String email,
                                       String fotoUrl, String telefone) {
        this.nome = Objects.requireNonNull(nome);
        this.email = Objects.requireNonNull(email);
        this.fotoUrl = Objects.requireNonNull(fotoUrl);
        this.telefone = Objects.requireNonNull(telefone);
    }

    public void definirSenhaDefinitiva(String novoHash, LocalDateTime instante) {
        this.senhaHash = Objects.requireNonNull(novoHash);
        this.primeiroAcessoPendente = false;
        this.senhaDefinitivaEm = Objects.requireNonNull(instante);
    }

    public void alterarSenha(String novoHash) {
        this.senhaHash = Objects.requireNonNull(novoHash);
    }

    public void alterarPerfil(TipoPerfil novoPerfil) {
        this.tipoPerfil = Objects.requireNonNull(novoPerfil);
    }

    public void desativar(LocalDateTime instante) {
        this.status = StatusUsuario.INATIVO;
        this.desativadoEm = Objects.requireNonNull(instante);
    }

    public void reativar() {
        this.status = StatusUsuario.ATIVO;
        this.desativadoEm = null;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getSenhaHash() { return senhaHash; }
    public String getLogin() { return login; }
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public String getFotoUrl() { return fotoUrl; }
    public String getTelefone() { return telefone; }
    public TipoPerfil getTipoPerfil() { return tipoPerfil; }
    public StatusUsuario getStatus() { return status; }
    public boolean isPrimeiroAcessoPendente() { return primeiroAcessoPendente; }
    public LocalDateTime getSenhaDefinitivaEm() { return senhaDefinitivaEm; }
    public Usuario getCriadoPor() { return criadoPor; }
    public LocalDateTime getDesativadoEm() { return desativadoEm; }
}
