package com.aclg.apecan.auth.service;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.shared.mail.EnvioEmail;
import com.aclg.apecan.shared.transaction.AposCommitExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(name = "apecan.redefinicao.entrega", havingValue = "email")
public class EntregaRedefinicaoSenhaEmail implements EntregaRedefinicaoSenha {
    private final EnvioEmail envio;
    private final String url;
    private final AposCommitExecutor aposCommit;
    public EntregaRedefinicaoSenhaEmail(EnvioEmail envio, @Value("${apecan.url-publica}") String url,
            AposCommitExecutor aposCommit) {
        this.envio = envio; this.url = url.replaceAll("/+$", ""); this.aposCommit = aposCommit;
    }
    public RedefinicaoEmitida entregar(Usuario usuario, String token) {
        String link = UriComponentsBuilder.fromUriString(url).path("/redefinir-senha")
                .queryParam("token", token).build().encode().toUriString();
        String destino = usuario.getEmail();
        aposCommit.executar(() -> envio.enviar(destino, "Redefinição de senha APECAN",
            "Redefina sua senha pelo link:\n" + link + "\nSe você não solicitou, ignore esta mensagem."),
            "envio de redefinicao de senha");
        return new RedefinicaoEmitida(null);
    }
}
