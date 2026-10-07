package com.aclg.apecan.auth.service;

import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.TokenCredencialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class RevogacaoTokensService {
    private final TokenCredencialRepository tokens;
    private final Clock clock;

    public RevogacaoTokensService(TokenCredencialRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    /** O chamador deve bloquear o usuário antes dos tokens, na mesma transação. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void revogarPendentes(Usuario usuario) {
        LocalDateTime agora = LocalDateTime.now(clock);
        tokens.findAllByUsuarioAndUtilizadoEmIsNull(usuario)
            .forEach(token -> token.invalidar(agora));
    }
}
