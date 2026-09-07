package com.aclg.apecan.auth.service;

import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.shared.transaction.AposCommitExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "apecan.ativacao.entrega", havingValue = "email")
public class EntregaAtivacaoEmail implements EntregaAtivacao {

	private final JavaMailSender mailSender;

	private final String urlPublica;

	private final String remetente;
	private final AposCommitExecutor aposCommit;

	public EntregaAtivacaoEmail(JavaMailSender mailSender, @Value("${apecan.url-publica}") String urlPublica,
			@Value("${APECAN_MAIL_FROM:${spring.mail.username}}") String remetente, AposCommitExecutor aposCommit) {
		this.mailSender = mailSender;
		this.urlPublica = urlPublica.replaceAll("/+$", "");
		this.remetente = remetente;
		this.aposCommit = aposCommit;
	}

	@Override
	public AtivacaoEmitida entregar(Usuario usuario, String tokenOriginal, LocalDateTime expiraEm) {
		String link = UriComponentsBuilder.fromUriString(urlPublica)
			.path("/ativar-conta")
			.queryParam("token", tokenOriginal)
			.build()
			.encode()
			.toUriString();
		aposCommit.executar(() -> enviar(usuario.getEmail(), "Ative sua conta APECAN", "Ola, " + usuario.getNome()
				+ ".\n\nDefina sua senha pelo link:\n" + link + "\n\nO link expira em " + expiraEm + "."),
				"envio de ativacao");
		return new AtivacaoEmitida(expiraEm, "Link enviado ao e-mail cadastrado.", null);
	}

	private void enviar(String destino, String assunto, String texto) {
		SimpleMailMessage mensagem = new SimpleMailMessage();
		mensagem.setFrom(remetente);
		mensagem.setTo(destino);
		mensagem.setSubject(assunto);
		mensagem.setText(texto);
		mailSender.send(mensagem);
	}

}
