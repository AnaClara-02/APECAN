package com.aclg.apecan.backup.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;
import static org.assertj.core.api.Assertions.*;

class RecuperacaoDisponibilidadeServiceTests {
    @Test void servidorNormalNuncaPermiteRecuperacaoMesmoComOrigemForjada() throws Exception {
        var service = new RecuperacaoDisponibilidadeService(new MockEnvironment(), false, "", "framework");
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.50"); request.addHeader("X-Forwarded-For", "127.0.0.1");
        new ForwardedHeaderFilter().doFilter(request, new MockHttpServletResponse(), (filtrada, resposta) -> {
            assertThat(service.permitida((jakarta.servlet.http.HttpServletRequest) filtrada)).isFalse();
        });
        request.setRemoteAddr("127.0.0.1");
        assertThat(service.permitida(request)).isFalse();
    }
    @Test void recuperacaoExplicitaExigeConexaoLocalSemConfiarEmCabecalhos() {
        var env = new MockEnvironment(); env.setActiveProfiles("recovery");
        var service = new RecuperacaoDisponibilidadeService(env, true, "127.0.0.1", "none");
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.50"); request.addHeader("X-Forwarded-For", "127.0.0.1");
        assertThat(service.permitida(request)).isFalse();
        request.setRemoteAddr("127.0.0.1"); assertThat(service.permitida(request)).isTrue();
    }
    @Test void inicializacaoRecusaConfiguracoesExpostas() {
        var env = new MockEnvironment(); env.setActiveProfiles("recovery");
        assertThatThrownBy(() -> new RecuperacaoDisponibilidadeService(env, true, "0.0.0.0", "none"))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new RecuperacaoDisponibilidadeService(env, true, "127.0.0.1", "framework"))
            .isInstanceOf(IllegalStateException.class);
        env.setActiveProfiles("recovery", "render");
        assertThatThrownBy(() -> new RecuperacaoDisponibilidadeService(env, true, "127.0.0.1", "none"))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new RecuperacaoDisponibilidadeService(new MockEnvironment(), true, "127.0.0.1", "none"))
            .isInstanceOf(IllegalStateException.class);
    }
}
