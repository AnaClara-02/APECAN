package com.aclg.apecan.usuario.dto;

import com.aclg.apecan.auth.service.AtivacaoEmitida;

public record UsuarioCriadoResultado(
    UsuarioResumoDto usuario,
    AtivacaoEmitida ativacao
) {
}
