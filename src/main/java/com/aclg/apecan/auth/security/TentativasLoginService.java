package com.aclg.apecan.auth.security;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;

@Service
public class TentativasLoginService {
    private final ControleAcessoStore store;
    private final Clock clock;
    private final int limite;
    private final Duration janela, bloqueioInicial, bloqueioMaximo;
    public TentativasLoginService(ControleAcessoStore store, Clock clock,
            @Value("${apecan.login.limite-tentativas:5}") int limite,
            @Value("${apecan.login.janela:15m}") Duration janela,
            @Value("${apecan.login.bloqueio-inicial:1m}") Duration bloqueioInicial,
            @Value("${apecan.login.bloqueio-maximo:15m}") Duration bloqueioMaximo) {
        this.store=store; this.clock=clock; this.limite=limite; this.janela=janela;
        this.bloqueioInicial=bloqueioInicial; this.bloqueioMaximo=bloqueioMaximo;
    }
    public void verificar(String login) {
        boolean bloqueado = store.alterar("LOGIN", login, estado -> estado.bloqueadoAte > clock.millis());
        if (bloqueado) throw new LockedException("Usuário ou senha inválidos.");
    }
    public void registrarFalha(String login) {
        store.alterar("LOGIN", login, estado -> {
            long agora=clock.millis();
            if (estado.falhas == 0 || agora >= estado.inicio + janela.toMillis()) {
                estado.falhas=0; estado.inicio=agora; estado.bloqueadoAte=0;
            }
            estado.falhas=Math.min(estado.falhas + 1, limite + 8);
            if (estado.falhas >= limite) {
                long tempo=Math.min(bloqueioInicial.toMillis() * (1L << Math.min(estado.falhas-limite, 8)),
                        bloqueioMaximo.toMillis());
                estado.bloqueadoAte=agora+tempo;
            }
            estado.ultimo=agora;
            estado.expira=Math.max(estado.inicio + janela.toMillis(), estado.bloqueadoAte);
            return null;
        });
    }
    public void registrarSucesso(String login) {
        store.alterar("LOGIN", login, estado -> {
            estado.falhas=0; estado.bloqueadoAte=0; estado.expira=clock.millis(); return null;
        });
    }
}
