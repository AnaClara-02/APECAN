package com.aclg.apecan.auth.security;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.aclg.apecan.usuario.entity.Usuario;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ReautenticacaoServiceTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    private final Clock clock = Clock.systemUTC();
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final Usuario usuario = mock(Usuario.class);
    private ControleAcessoStore store;
    @BeforeEach void preparar() {
        jdbc.update("DELETE FROM controles_acesso");
        store = new ControleAcessoStore(jdbc, manager, clock, "segredo-ficticio-testes");
        when(usuario.getId()).thenReturn(91234L); when(usuario.estaAtivado()).thenReturn(true);
        when(usuario.getSenhaHash()).thenReturn("hash-ficticio");
        when(encoder.matches("correta", "hash-ficticio")).thenReturn(true);
    }
    @Test void limiteSobreviveRollbackENovaInstanciaEExpiraSemCalcularHashDuranteBloqueio() {
        var service = new ReautenticacaoService(store, encoder, clock);
        for (int i=0; i<5; i++) assertThatThrownBy(() -> new TransactionTemplate(manager).execute(status -> {
            service.validar(usuario, "errada"); return null;
        })).isInstanceOf(com.aclg.apecan.shared.exception.OperacaoInvalidaException.class);
        assertThatThrownBy(() -> new ReautenticacaoService(store, encoder, clock).validar(usuario, "correta"))
            .isInstanceOf(com.aclg.apecan.shared.exception.OperacaoInvalidaException.class);
        verify(encoder, never()).matches("correta", "hash-ficticio");
        assertThatCode(() -> new ReautenticacaoService(store, encoder, Clock.offset(clock, Duration.ofMinutes(16)))
            .validar(usuario, "correta")).doesNotThrowAnyException();
    }
    @Test void tentativasConcorrentesCompartilhamOLimiteAntesDoBCrypt() throws Exception {
        var service = new ReautenticacaoService(store, encoder, clock);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var tarefas = java.util.stream.IntStream.range(0, 10).<java.util.concurrent.Callable<Void>>mapToObj(i -> () -> {
                assertThatThrownBy(() -> service.validar(usuario, "errada"))
                    .isInstanceOf(com.aclg.apecan.shared.exception.OperacaoInvalidaException.class);
                return null;
            }).toList();
            for (var resultado : executor.invokeAll(tarefas)) resultado.get();
        }
        verify(encoder, times(5)).matches("errada", "hash-ficticio");
    }
}
