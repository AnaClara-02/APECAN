package com.aclg.apecan.auth.security;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.usuario.entity.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Clock;

/** Limite compartilhado por todas as ações sensíveis, persistido mesmo em rollback. */
@Service
public class ReautenticacaoService {
    private static final long JANELA = 15 * 60 * 1000L;
    private static final int LIMITE = 5;
    private final ControleAcessoStore store;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public ReautenticacaoService(ControleAcessoStore store, PasswordEncoder encoder, Clock clock) {
        this.store = store; this.encoder = encoder; this.clock = clock;
    }

    public void validar(Usuario usuario, String senha) {
        boolean aceita = store.alterar("REAUTH", usuario.getId().toString(), estado -> {
            long agora = clock.millis();
            if (agora < estado.bloqueadoAte) return false;
            if (agora >= estado.inicio + JANELA) {
                estado.falhas = 0; estado.inicio = agora;
            }
            // Serializa tentativas entre instâncias antes de calcular BCrypt.
            boolean confere = senha != null && usuario.estaAtivado() && usuario.getSenhaHash() != null
                    && encoder.matches(senha, usuario.getSenhaHash());
            estado.ultimo = agora;
            estado.expira = agora + JANELA;
            if (confere) {
                estado.falhas = 0; estado.bloqueadoAte = 0;
            } else if (++estado.falhas >= LIMITE) {
                estado.bloqueadoAte = agora + JANELA;
            }
            return confere;
        });
        // A exceção fica fora da transação independente que persiste o contador.
        if (!aceita) throw new OperacaoInvalidaException("SENHA_ATUAL_INVALIDA",
                "Não foi possível confirmar a senha atual. Verifique os dados ou aguarde 15 minutos.");
    }
}
