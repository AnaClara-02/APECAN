package com.aclg.apecan.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@Profile("prod")
public class ProducaoSeguraValidator implements org.springframework.beans.factory.InitializingBean {

	@org.springframework.beans.factory.annotation.Autowired
	private org.springframework.core.env.Environment env;

	private final String urlPublica;

	private final boolean cookieSeguro;

	private final String entregaAtivacao;

	private final String entregaRedefinicao;

	public ProducaoSeguraValidator(@Value("${apecan.url-publica}") String urlPublica,
			@Value("${server.servlet.session.cookie.secure}") boolean cookieSeguro,
			@Value("${apecan.ativacao.entrega}") String entregaAtivacao,
			@Value("${apecan.redefinicao.entrega}") String entregaRedefinicao) {
		this.urlPublica = urlPublica;
		this.cookieSeguro = cookieSeguro;
		this.entregaAtivacao = entregaAtivacao;
		this.entregaRedefinicao = entregaRedefinicao;
	}

	@Override
	public void afterPropertiesSet() {
		URI uri = URI.create(urlPublica);
		if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null ||
				uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
			throw new IllegalStateException("Producao exige APECAN_PUBLIC_URL com HTTPS.");
		}
		if (!cookieSeguro) {
			throw new IllegalStateException("Producao exige cookie de sessao seguro.");
		}
		if (!"email".equals(entregaAtivacao) || !"email".equals(entregaRedefinicao)) {
			throw new IllegalStateException("Producao exige entrega de credenciais por e-mail.");
		}
		String chave = env.getProperty("apecan.acesso.chave-hmac", "");
		if (chave.length() < 32 || chave.equals("apecan-desenvolvimento-nao-usar-em-producao")) {
			throw new IllegalStateException("Producao exige APECAN_ACCESS_HMAC_KEY secreta com ao menos 32 caracteres.");
		}
		String transporte = env.getProperty("apecan.mail.transporte", "smtp");
		String remetente = env.getProperty("APECAN_MAIL_FROM", "");
		if (remetente.isBlank() || !remetente.contains("@") || remetente.contains("\n") || remetente.contains("\r")) {
			throw new IllegalStateException("Producao exige remetente de e-mail configurado.");
		}
		if ("brevo".equals(transporte)) {
			if (env.getProperty("APECAN_BREVO_API_KEY", "").isBlank())
				throw new IllegalStateException("Configure a chave da API Brevo.");
		} else if ("smtp".equals(transporte)) {
			if (env.getProperty("spring.mail.username", "").isBlank() ||
					env.getProperty("spring.mail.password", "").isBlank() ||
					!env.getProperty("spring.mail.properties.mail.smtp.starttls.required", Boolean.class, false))
				throw new IllegalStateException("SMTP em producao exige credenciais e TLS.");
		} else throw new IllegalStateException("Transporte de e-mail desconhecido.");
	}

}
