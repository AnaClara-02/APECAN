package com.aclg.apecan.doacao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DoacaoVoluntarioId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "id_doacao")
    private Long doacaoId;

    @Column(name = "id_voluntario")
    private Long voluntarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", length = 20)
    private PapelVoluntario papel;

    protected DoacaoVoluntarioId() {
    }

    public DoacaoVoluntarioId(Long doacaoId, Long voluntarioId,
                              PapelVoluntario papel) {
        this.doacaoId = doacaoId;
        this.voluntarioId = voluntarioId;
        this.papel = Objects.requireNonNull(papel);
    }

    public Long getDoacaoId() { return doacaoId; }
    public Long getVoluntarioId() { return voluntarioId; }
    public PapelVoluntario getPapel() { return papel; }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) return true;
        if (!(outro instanceof DoacaoVoluntarioId that)) return false;
        return Objects.equals(doacaoId, that.doacaoId)
            && Objects.equals(voluntarioId, that.voluntarioId)
            && papel == that.papel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(doacaoId, voluntarioId, papel);
    }
}
