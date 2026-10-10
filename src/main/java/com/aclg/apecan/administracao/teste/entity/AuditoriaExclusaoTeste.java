package com.aclg.apecan.administracao.teste.entity;

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

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "auditoria_exclusoes_teste")
public class AuditoriaExclusaoTeste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_responsavel_usuario", nullable = false,
        foreignKey = @ForeignKey(name = "fk_auditoria_exclusoes_responsavel"))
    private Usuario responsavel;

    @Column(name = "tipo_registro", nullable = false, length = 30)
    private String tipoRegistro;

    @Column(name = "id_registro", nullable = false)
    private Long idRegistro;

    @Column(nullable = false, length = 500)
    private String justificativa;

    @Column(name = "quantidade_removida", nullable = false)
    private int quantidadeRemovida;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private LocalDateTime ocorridoEm;

    protected AuditoriaExclusaoTeste() { }

    public AuditoriaExclusaoTeste(Usuario responsavel, String tipoRegistro, Long idRegistro,
            String justificativa, int quantidadeRemovida, LocalDateTime ocorridoEm) {
        this.responsavel = Objects.requireNonNull(responsavel);
        this.tipoRegistro = Objects.requireNonNull(tipoRegistro);
        this.idRegistro = Objects.requireNonNull(idRegistro);
        this.justificativa = Objects.requireNonNull(justificativa);
        this.quantidadeRemovida = quantidadeRemovida;
        this.ocorridoEm = Objects.requireNonNull(ocorridoEm);
    }
}
