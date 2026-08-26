package com.aclg.apecan.auth.service;

import com.aclg.apecan.usuario.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(name = "apecan.redefinicao.entrega", havingValue = "local", matchIfMissing = true)
public class EntregaRedefinicaoSenhaLocal implements EntregaRedefinicaoSenha {

	private final String urlPublica;

	public EntregaRedefinicaoSenhaLocal(
			@Value("${apecan.url-publica:http://localhost:${server.port:8080}}") String urlPublica) {
		this.urlPublica = urlPublica.replaceAll("/+$", "");
	}

	@Override
	public RedefinicaoEmitida entregar(Usuario usuario, String tokenOriginal) {
		String link = UriComponentsBuilder.fromUriString(urlPublica)
			.path("/redefinir-senha")
			.queryParam("token", tokenOriginal)
			.build()
			.encode()
			.toUriString();
		return new RedefinicaoEmitida(link);
	}

}
