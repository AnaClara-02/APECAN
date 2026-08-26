package com.aclg.apecan.auth.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class EventosAutenticacaoListener {

	private final TentativasLoginService tentativas;

	public EventosAutenticacaoListener(TentativasLoginService tentativas) {
		this.tentativas = tentativas;
	}

	@EventListener
	public void falha(AuthenticationFailureBadCredentialsEvent evento) {
		tentativas.registrarFalha(evento.getAuthentication().getName());
	}

	@EventListener
	public void sucesso(AuthenticationSuccessEvent evento) {
		tentativas.registrarSucesso(evento.getAuthentication().getName());
	}

}
