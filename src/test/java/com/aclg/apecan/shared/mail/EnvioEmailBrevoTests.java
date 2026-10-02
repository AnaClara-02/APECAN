package com.aclg.apecan.shared.mail;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;

class EnvioEmailBrevoTests {
    @Test void enviaPorHttpsComChaveEApenasDadosNecessarios() {
        var builder=RestClient.builder().baseUrl("https://api.brevo.com").defaultHeader("api-key","chave-ficticia");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("api-key","chave-ficticia"))
            .andExpect(jsonPath("$.sender.email").value("sistema@example.invalid"))
            .andExpect(jsonPath("$.to[0].email").value("destino@example.invalid"))
            .andRespond(withStatus(HttpStatus.CREATED));
        new EnvioEmailBrevo(builder.build(),"sistema@example.invalid")
            .enviar("destino@example.invalid","Ativação","Texto de teste");
        server.verify();
    }
    @Test void falhasNaoVazamRespostaNemSaoRepetidas() {
        for (HttpStatus status : new HttpStatus[]{HttpStatus.UNAUTHORIZED,HttpStatus.TOO_MANY_REQUESTS,
                HttpStatus.INTERNAL_SERVER_ERROR}) {
            var builder=RestClient.builder().baseUrl("https://api.brevo.com");
            var server=MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withStatus(status).body("dado-sensivel-de-teste"));
            assertThatThrownBy(() -> new EnvioEmailBrevo(builder.build(),"remetente@example.invalid")
                .enviar("destino@example.invalid","Teste","Texto"))
                .hasMessage("ENVIO_EMAIL_INDISPONIVEL").hasNoCause();
            server.verify();
        }
    }
    @Test void timeoutNaoEhRepetidoNemExposto() {
        var builder=RestClient.builder().baseUrl("https://api.brevo.com");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
            .andRespond(withException(new java.net.SocketTimeoutException("segredo-ficticio")));
        assertThatThrownBy(() -> new EnvioEmailBrevo(builder.build(),"remetente@example.invalid")
            .enviar("destino@example.invalid","Teste","Texto"))
            .hasMessage("ENVIO_EMAIL_INDISPONIVEL").hasNoCause();
        server.verify();
    }
}
