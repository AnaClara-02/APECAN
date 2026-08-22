package com.aclg.apecan.auth.service;

import java.time.LocalDateTime;

public record TokenAtivacaoInfo(
    String nomeUsuario,
    LocalDateTime expiraEm
) {
}
