package com.aclg.apecan.auth.security;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.LockedException;

@SpringBootTest
class TentativasLoginServiceTests {
    ControleAcessoStore store;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneOffset.UTC);
    @org.junit.jupiter.api.BeforeEach void preparar() {
        jdbc.update("DELETE FROM controles_acesso");
        store = new ControleAcessoStore(jdbc,manager,clock,"chave-ficticia-para-testes");
    }
    private TentativasLoginService servico() {
        return new TentativasLoginService(store, clock, 5, Duration.ofMinutes(15),
            Duration.ofMinutes(1), Duration.ofMinutes(15));
    }
    @Test void bloqueioSobreviveANovaInstanciaESucessoRemove() {
        var service = servico();
        for (int i=0;i<5;i++) service.registrarFalha(" Usuario.Teste ");
        assertThatThrownBy(() -> servico().verificar("usuario.teste")).isInstanceOf(LockedException.class);
        service.registrarSucesso("USUARIO.TESTE");
        assertThatCode(() -> service.verificar("usuario.teste")).doesNotThrowAnyException();
        assertThat(jdbc.queryForList("SELECT chave FROM controles_acesso", String.class))
            .noneMatch(chave -> chave.contains("usuario") || chave.contains("@"));
    }
    @Test void recuperacaoLimitaIntervaloEQuantidade() {
        var t0=Clock.offset(Clock.systemUTC(), Duration.ofHours(2));
        var servico=new LimiteRecuperacaoService(store,t0);
        String email="limites@example.invalid";
        assertThat(servico.permitir(email)).isTrue();
        assertThat(servico.permitir(email)).isFalse();
        assertThat(new LimiteRecuperacaoService(store, Clock.offset(t0,Duration.ofMinutes(1))).permitir(email)).isTrue();
        assertThat(new LimiteRecuperacaoService(store, Clock.offset(t0,Duration.ofMinutes(2))).permitir(email)).isTrue();
        assertThat(new LimiteRecuperacaoService(store, Clock.offset(t0,Duration.ofMinutes(3))).permitir(email)).isFalse();
        assertThat(new LimiteRecuperacaoService(store, Clock.offset(t0,Duration.ofHours(1))).permitir(email)).isTrue();
    }
    @Test void solicitacoesConcorrentesCompartilhamMesmoLimite() throws Exception {
        var service = new LimiteRecuperacaoService(store,clock);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var tarefas = java.util.stream.IntStream.range(0,8)
                .<java.util.concurrent.Callable<Boolean>>mapToObj(i -> () -> service.permitir("concorrente@example.invalid"))
                .toList();
            int aceitas = 0;
            for (var resultado : executor.invokeAll(tarefas)) if (resultado.get()) aceitas++;
            assertThat(aceitas).isEqualTo(1);
        }
    }
}
