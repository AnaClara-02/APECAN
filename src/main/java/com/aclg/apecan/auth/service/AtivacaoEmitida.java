package com.aclg.apecan.auth.service;

import java.time.LocalDateTime;

public record AtivacaoEmitida(
    LocalDateTime expiraEm,
    String mensagem,
    String linkLocal
) {
}
