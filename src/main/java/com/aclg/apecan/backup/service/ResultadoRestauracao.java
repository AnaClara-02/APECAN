package com.aclg.apecan.backup.service;

import java.time.LocalDateTime;

public record ResultadoRestauracao(String versaoBackup, LocalDateTime restauradoEm, boolean reinicioAgendado) {
}
