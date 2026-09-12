package com.aclg.apecan.auth.security;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

@Service
public class SessaoUsuarioService {

    private final SessionRegistry sessionRegistry;

    public SessaoUsuarioService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void encerrarSessoes(Long usuarioId) {
        sessionRegistry.getAllPrincipals().stream()
            .filter(UsuarioPrincipal.class::isInstance)
            .map(UsuarioPrincipal.class::cast)
            .filter(principal -> usuarioId.equals(principal.getId()))
            .forEach(principal -> sessionRegistry
                .getAllSessions(principal, false)
                .forEach(SessionInformation::expireNow));
    }

    public void encerrarTodasSessoes() {
        sessionRegistry.getAllPrincipals().forEach(principal -> sessionRegistry
            .getAllSessions(principal, false)
            .forEach(SessionInformation::expireNow));
    }
}
