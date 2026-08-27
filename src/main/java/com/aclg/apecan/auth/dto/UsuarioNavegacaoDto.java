package com.aclg.apecan.auth.dto;

import com.aclg.apecan.usuario.entity.TipoPerfil;

public record UsuarioNavegacaoDto(Long id, String nome, String login, TipoPerfil tipoPerfil) {

	public boolean administrador() {
		return tipoPerfil == TipoPerfil.ADMINISTRADOR;
	}
}
