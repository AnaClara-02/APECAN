package com.aclg.apecan.shared.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "apecan.mail.transporte", havingValue = "smtp", matchIfMissing = true)
public class EnvioEmailSmtp implements EnvioEmail {
    private final JavaMailSender sender;
    private final String remetente;
    public EnvioEmailSmtp(JavaMailSender sender,
            @Value("${APECAN_MAIL_FROM:${spring.mail.username:}}") String remetente) {
        this.sender = sender;
        this.remetente = remetente;
    }
    public void enviar(String destino, String assunto, String texto) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(destino);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        sender.send(mensagem);
    }
}

