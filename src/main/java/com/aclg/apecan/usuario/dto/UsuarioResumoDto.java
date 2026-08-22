package com.aclg.apecan.usuario.dto;

import com.aclg.apecan.usuario.entity.StatusUsuario;
import com.aclg.apecan.usuario.entity.TipoPerfil;

public record UsuarioResumoDto(
    Long id,
    String nome,
    String login,
    String cpf,
    String email,
    String telefone,
    TipoPerfil tipoPerfil,
    StatusUsuario status,
    boolean primeiroAcessoPendente
) {
    public boolean estaAtivo() {
        return status == StatusUsuario.ATIVO;
    }

    public boolean estaAtivado() {
        return !primeiroAcessoPendente;
    }
}
