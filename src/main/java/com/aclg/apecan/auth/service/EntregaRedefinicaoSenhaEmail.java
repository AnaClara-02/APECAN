package com.aclg.apecan.auth.service;

import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.shared.transaction.AposCommitExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(name = "apecan.redefinicao.entrega", havingValue = "email")
public class EntregaRedefinicaoSenhaEmail implements EntregaRedefinicaoSenha {

	private final JavaMailSender mailSender;

	private final String urlPublica;

	private final String remetente;
	private final AposCommitExecutor aposCommit;

	public EntregaRedefinicaoSenhaEmail(JavaMailSender mailSender, @Value("${apecan.url-publica}") String urlPublica,
			@Value("${APECAN_MAIL_FROM:${spring.mail.username}}") String remetente, AposCommitExecutor aposCommit) {
		this.mailSender = mailSender;
		this.urlPublica = urlPublica.replaceAll("/+$", "");
		this.remetente = remetente;
		this.aposCommit = aposCommit;
	}

	@Override
	public RedefinicaoEmitida entregar(Usuario usuario, String tokenOriginal) {
		String link = UriComponentsBuilder.fromUriString(urlPublica)
			.path("/redefinir-senha")
			.queryParam("token", tokenOriginal)
			.build()
			.encode()
			.toUriString();
		SimpleMailMessage mensagem = new SimpleMailMessage();
		mensagem.setFrom(remetente);
		mensagem.setTo(usuario.getEmail());
		mensagem.setSubject("Redefinicao de senha APECAN");
		mensagem.setText("Ola, " + usuario.getNome() + ".\n\nRedefina sua senha pelo link:\n" + link
				+ "\n\nSe voce nao solicitou, ignore esta mensagem.");
		aposCommit.executar(() -> mailSender.send(mensagem), "envio de redefinicao de senha");
		return new RedefinicaoEmitida(null);
	}

}
