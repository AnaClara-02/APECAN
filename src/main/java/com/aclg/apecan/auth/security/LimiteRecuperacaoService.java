package com.aclg.apecan.auth.security;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class LimiteRecuperacaoService {
    private final ControleAcessoStore store;
    private final Clock clock;
    public LimiteRecuperacaoService(ControleAcessoStore store, Clock clock) {
        this.store = store; this.clock = clock;
    }
    public boolean permitir(String identificador) {
        return store.alterar("RESET", identificador, estado -> {
            long agora = clock.millis();
            if (estado.falhas == 0 || agora >= estado.inicio + 3600000) {
                estado.falhas = 0; estado.inicio = agora;
            }
            if (estado.falhas >= 3 || (estado.falhas > 0 && agora < estado.ultimo + 60000)) return false;
            estado.falhas++;
            estado.ultimo = agora;
            estado.expira = estado.inicio + 3600000;
            return true;
        });
    }
}
