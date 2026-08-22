package com.aclg.apecan.auth.service;

import com.aclg.apecan.usuario.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(
    name = "apecan.ativacao.entrega",
    havingValue = "local",
    matchIfMissing = true
)
public class EntregaAtivacaoLocal implements EntregaAtivacao {

    private final String urlPublica;

    public EntregaAtivacaoLocal(
            @Value("${apecan.url-publica:http://localhost:${server.port:8080}}")
            String urlPublica) {
        this.urlPublica = urlPublica.replaceAll("/+$", "");
    }

    @Override
    public AtivacaoEmitida entregar(
            Usuario usuario,
            String tokenOriginal,
            LocalDateTime expiraEm) {
        String link = UriComponentsBuilder.fromUriString(urlPublica)
            .path("/ativar-conta")
            .queryParam("token", tokenOriginal)
            .build()
            .encode()
            .toUriString();

        return new AtivacaoEmitida(
            expiraEm,
            "Entregue este link ao usuario. Ele sera exibido somente agora.",
            link
        );
    }
}
