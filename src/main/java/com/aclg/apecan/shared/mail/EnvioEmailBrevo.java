package com.aclg.apecan.shared.mail;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "apecan.mail.transporte", havingValue = "brevo")
public class EnvioEmailBrevo implements EnvioEmail {
    private final RestClient client;
    private final String remetente;

    @org.springframework.beans.factory.annotation.Autowired
    public EnvioEmailBrevo(@Value("${APECAN_BREVO_API_KEY:}") String chave,
            @Value("${APECAN_MAIL_FROM:}") String remetente) {
        this(criarClient(chave), remetente);
    }

    // Ponto de injecao para testes: o destino real nunca e configuravel por requisicao.
    EnvioEmailBrevo(RestClient client, String remetente) {
        this.client = client;
        this.remetente = remetente;
    }

    static RestClient criarClient(String chave) {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl("https://api.brevo.com")
                .requestFactory(factory).defaultHeader("api-key", chave).build();
    }

    @Override
    public void enviar(String destino, String assunto, String texto) {
        try {
            client.post().uri("/v3/smtp/email").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("sender", Map.of("email", remetente, "name", "APECAN"),
                    "to", List.of(Map.of("email", destino)), "subject", assunto, "textContent", texto))
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() != 201) {
                        throw new IllegalStateException("ENVIO_EMAIL_RECUSADO");
                    }
                    return null; // Nao ler ou registrar corpo de erro/conteudo sensivel.
                });
        } catch (RuntimeException exception) {
            // Nao preservar a causa: clientes HTTP podem incluir respostas e dados pessoais.
            throw new IllegalStateException("ENVIO_EMAIL_INDISPONIVEL");
        }
    }
}
