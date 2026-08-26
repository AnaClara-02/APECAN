package com.aclg.apecan.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TentativasLoginService {

	private final ConcurrentHashMap<String, Tentativa> tentativas = new ConcurrentHashMap<>();

	private final Clock clock;

	private final int limite;

	private final Duration janela;

	private final Duration bloqueioInicial;

	private final Duration bloqueioMaximo;

	public TentativasLoginService(Clock clock, @Value("${apecan.login.limite-tentativas:5}") int limite,
			@Value("${apecan.login.janela:15m}") Duration janela,
			@Value("${apecan.login.bloqueio-inicial:1m}") Duration bloqueioInicial,
			@Value("${apecan.login.bloqueio-maximo:15m}") Duration bloqueioMaximo) {
		this.clock = clock;
		this.limite = limite;
		this.janela = janela;
		this.bloqueioInicial = bloqueioInicial;
		this.bloqueioMaximo = bloqueioMaximo;
	}

	public void verificar(String login) {
		String chave = normalizar(login);
		Tentativa tentativa = tentativas.get(chave);
		if (tentativa == null)
			return;
		Instant agora = clock.instant();
		if (tentativa.inicioJanela().plus(janela).isBefore(agora)) {
			tentativas.remove(chave, tentativa);
		}
		else if (tentativa.bloqueadoAte() != null && tentativa.bloqueadoAte().isAfter(agora)) {
			throw new LockedException("Usuário ou senha inválidos.");
		}
	}

	public void registrarFalha(String login) {
		String chave = normalizar(login);
		Instant agora = clock.instant();
		tentativas.compute(chave, (ignorada, anterior) -> {
			boolean novaJanela = anterior == null || anterior.inicioJanela().plus(janela).isBefore(agora);
			int falhas = novaJanela ? 1 : anterior.falhas() + 1;
			Instant inicio = novaJanela ? agora : anterior.inicioJanela();
			Instant bloqueadoAte = null;
			if (falhas >= limite) {
				long multiplicador = 1L << Math.min(falhas - limite, 8);
				Duration bloqueio = bloqueioInicial.multipliedBy(multiplicador);
				if (bloqueio.compareTo(bloqueioMaximo) > 0)
					bloqueio = bloqueioMaximo;
				bloqueadoAte = agora.plus(bloqueio);
			}
			return new Tentativa(falhas, inicio, bloqueadoAte);
		});
	}

	public void registrarSucesso(String login) {
		tentativas.remove(normalizar(login));
	}

	private String normalizar(String login) {
		return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
	}

	private record Tentativa(int falhas, Instant inicioJanela, Instant bloqueadoAte) {
	}

}
