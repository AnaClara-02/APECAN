package com.aclg.apecan.relatorio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record LimitesExportacao(int registros, long bytes, int segundos) {
    public LimitesExportacao(@Value("${apecan.relatorios.max-registros:10000}") int registros,
            @Value("${apecan.relatorios.max-bytes:20971520}") long bytes,
            @Value("${apecan.relatorios.timeout-segundos:60}") int segundos) {
        if (registros < 1 || registros > 50000 || bytes < 1 || bytes > 52428800
                || segundos < 1 || segundos > 120)
            throw new IllegalArgumentException("Limites de relatório fora da faixa segura.");
        this.registros = registros; this.bytes = bytes; this.segundos = segundos;
    }
}
