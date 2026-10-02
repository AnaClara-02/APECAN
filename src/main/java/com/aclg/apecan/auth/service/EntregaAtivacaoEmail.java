package com.aclg.apecan.auth.service;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.shared.mail.EnvioEmail;
import com.aclg.apecan.shared.transaction.AposCommitExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "apecan.ativacao.entrega", havingValue = "email")
public class EntregaAtivacaoEmail implements EntregaAtivacao {
    private final EnvioEmail envio;
    private final String url;
    private final AposCommitExecutor aposCommit;
    public EntregaAtivacaoEmail(EnvioEmail envio, @Value("${apecan.url-publica}") String url,
            AposCommitExecutor aposCommit) {
        this.envio = envio; this.url = url.replaceAll("/+$", ""); this.aposCommit = aposCommit;
    }
    public AtivacaoEmitida entregar(Usuario usuario, String token, LocalDateTime expiraEm) {
        String link = UriComponentsBuilder.fromUriString(url).path("/ativar-conta")
                .queryParam("token", token).build().encode().toUriString();
        String destino = usuario.getEmail();
        AtivacaoEmitida resultado = new AtivacaoEmitida(expiraEm, "Envio aguardando confirmação.", null);
        aposCommit.executar(() -> {
            envio.enviar(destino, "Ative sua conta APECAN",
                "Defina sua senha pelo link:\n" + link + "\nO link expira em " + expiraEm + ".");
            resultado.envioAceito();
        }, "envio de ativacao", resultado::envioFalhou);
        return resultado;
    }
}
