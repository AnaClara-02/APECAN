package com.aclg.apecan.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@Profile("prod")
public class ProducaoSeguraValidator implements ApplicationRunner {

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
	public void run(ApplicationArguments args) {
		if (!"https".equalsIgnoreCase(URI.create(urlPublica).getScheme())) {
			throw new IllegalStateException("Producao exige APECAN_PUBLIC_URL com HTTPS.");
		}
		if (!cookieSeguro) {
			throw new IllegalStateException("Producao exige cookie de sessao seguro.");
		}
		if (!"email".equals(entregaAtivacao) || !"email".equals(entregaRedefinicao)) {
			throw new IllegalStateException("Producao exige entrega de credenciais por e-mail.");
		}
	}

}
