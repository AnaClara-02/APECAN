package com.aclg.apecan.backup.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Service
public class RecuperacaoDisponibilidadeService {
    private final boolean modoRecuperacao;
    public RecuperacaoDisponibilidadeService(Environment environment,
            @Value("${apecan.recovery.enabled:false}") boolean habilitada,
            @Value("${server.address:}") String endereco,
            @Value("${server.forward-headers-strategy:framework}") String encaminhamento) {
        if (habilitada && (!environment.acceptsProfiles(Profiles.of("recovery"))
                || environment.acceptsProfiles(Profiles.of("render"))
                || !loopback(endereco) || !"none".equalsIgnoreCase(encaminhamento))) {
            throw new IllegalStateException("Recuperação exige perfil recovery, listener loopback e forward-headers-strategy=none, fora de Render.");
        }
        this.modoRecuperacao = habilitada;
    }
    public boolean permitida(HttpServletRequest request) {
        // Não habilitar em banco vazio; o listener dedicado não interpreta cabeçalhos.
        return modoRecuperacao && loopback(request.getRemoteAddr());
    }
    public boolean modoRecuperacao() { return modoRecuperacao; }
    private static boolean loopback(String endereco) {
        return "127.0.0.1".equals(endereco) || "::1".equals(endereco)
                || "0:0:0:0:0:0:0:1".equals(endereco);
    }
}
