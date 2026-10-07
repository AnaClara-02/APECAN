package com.aclg.apecan.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessaoUsuarioServiceTests {
    @Test void revogaApenasAposCommit() {
        var registry = new SessionRegistryImpl();
        var principal = mock(UsuarioPrincipal.class);
        when(principal.getId()).thenReturn(42L);
        registry.registerNewSession("ficticia", principal);
        var service = new SessaoUsuarioService(registry);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            service.encerrarSessoes(42L);
            assertThat(registry.getSessionInformation("ficticia").isExpired()).isFalse();
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            assertThat(registry.getSessionInformation("ficticia").isExpired()).isTrue();
        } finally { TransactionSynchronizationManager.clear(); }
    }
}
