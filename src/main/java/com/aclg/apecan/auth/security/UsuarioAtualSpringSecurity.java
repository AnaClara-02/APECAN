package com.aclg.apecan.auth.security;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioAtualSpringSecurity implements UsuarioAtual {

    @Override
    public Long exigirId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new OperacaoInvalidaException(
                "USUARIO_NAO_AUTENTICADO",
                "Não foi possível identificar o usuário autenticado."
            );
        }
        return principal.getId();
    }
}
