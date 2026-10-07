package com.aclg.apecan.auth.service;

import com.aclg.apecan.usuario.entity.Usuario;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Sem canal de e-mail, a recuperação pública permanece desabilitada. */
@Component
@ConditionalOnProperty(name = "apecan.redefinicao.entrega", havingValue = "local", matchIfMissing = true)
public class EntregaRedefinicaoSenhaLocal implements EntregaRedefinicaoSenha {
    @Override
    public RedefinicaoEmitida entregar(Usuario usuario, String tokenOriginal) {
        // Nunca retornar, registrar em log ou publicar tokens no modo local.
        return new RedefinicaoEmitida(null);
    }
}
