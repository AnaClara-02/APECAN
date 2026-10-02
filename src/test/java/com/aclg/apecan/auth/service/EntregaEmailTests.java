package com.aclg.apecan.auth.service;
import com.aclg.apecan.shared.mail.EnvioEmail;
import com.aclg.apecan.shared.transaction.AposCommitExecutor;
import com.aclg.apecan.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class EntregaEmailTests {
    @Test void enviaSomenteAposCommitEAtualizaResultado() {
        var envio=mock(EnvioEmail.class);
        var usuario=mock(Usuario.class);
        when(usuario.getEmail()).thenReturn("teste@example.invalid");
        TransactionSynchronizationManager.initSynchronization();
        try {
            var entrega=new EntregaAtivacaoEmail(envio,"https://apecan.example.invalid",new AposCommitExecutor());
            var resultado=entrega.entregar(usuario,"token-ficticio",LocalDateTime.of(2026,9,26,12,0));
            verifyNoInteractions(envio);
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(envio).enviar(eq("teste@example.invalid"),anyString(),contains("https://apecan.example.invalid/ativar-conta"));
            assertThat(resultado.mensagem()).contains("aceito").doesNotContain("token-ficticio");
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void rollbackNaoEnviaEFalhaDoProvedorNaoPrometeEntrega() {
        var envio=mock(EnvioEmail.class);
        var usuario=mock(Usuario.class);
        when(usuario.getEmail()).thenReturn("teste@example.invalid");
        var entrega=new EntregaAtivacaoEmail(envio,"https://apecan.example.invalid",new AposCommitExecutor());
        TransactionSynchronizationManager.initSynchronization();
        try {
            entrega.entregar(usuario,"token-ficticio",LocalDateTime.now());
            TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verifyNoInteractions(envio);
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
        doThrow(new IllegalStateException("segredo")).when(envio).enviar(anyString(),anyString(),anyString());
        var resultado=entrega.entregar(usuario,"token-ficticio",LocalDateTime.now());
        assertThat(resultado.mensagem()).contains("não foi confirmado").doesNotContain("segredo");
    }
}
