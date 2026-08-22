package com.aclg.apecan.auth.service;

import com.aclg.apecan.usuario.entity.Usuario;

import java.time.LocalDateTime;

public interface EntregaAtivacao {

    AtivacaoEmitida entregar(
        Usuario usuario,
        String tokenOriginal,
        LocalDateTime expiraEm
    );
}
