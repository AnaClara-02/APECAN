package com.aclg.apecan.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class EntidadeAuditavel extends EntidadeCriada {

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
