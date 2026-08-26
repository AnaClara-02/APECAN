package com.aclg.apecan.auth.service;

import com.aclg.apecan.usuario.entity.Usuario;

public interface EntregaRedefinicaoSenha {

	RedefinicaoEmitida entregar(Usuario usuario, String tokenOriginal);

}
