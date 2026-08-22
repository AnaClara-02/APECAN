package com.aclg.apecan.doacao.entity;

import com.aclg.apecan.voluntario.entity.Voluntario;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "doacao_voluntarios")
public class DoacaoVoluntario {

    @EmbeddedId
    private DoacaoVoluntarioId id;

    @MapsId("doacaoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_doacao", nullable = false,
        foreignKey = @ForeignKey(name = "fk_doacao_voluntarios_doacao"))
    private Doacao doacao;

    @MapsId("voluntarioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_voluntario", nullable = false,
        foreignKey = @ForeignKey(name = "fk_doacao_voluntarios_voluntario"))
    private Voluntario voluntario;

    protected DoacaoVoluntario() {
    }

    public DoacaoVoluntario(Doacao doacao, Voluntario voluntario,
                            PapelVoluntario papel) {
        this.doacao = Objects.requireNonNull(doacao);
        this.voluntario = Objects.requireNonNull(voluntario);
        this.id = new DoacaoVoluntarioId(
            doacao.getId(), voluntario.getId(), Objects.requireNonNull(papel)
        );
    }

    public DoacaoVoluntarioId getId() { return id; }
    public Doacao getDoacao() { return doacao; }
    public Voluntario getVoluntario() { return voluntario; }
    public PapelVoluntario getPapel() { return id.getPapel(); }
}
