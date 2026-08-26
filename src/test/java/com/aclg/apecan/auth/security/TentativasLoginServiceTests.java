package com.aclg.apecan.auth.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.LockedException;

class TentativasLoginServiceTests {

	@Test
	void deveBloquearProgressivamenteERemoverBloqueioAposSucesso() {
		TentativasLoginService service = new TentativasLoginService(
				Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneId.of("America/Sao_Paulo")), 5,
				Duration.ofMinutes(15), Duration.ofMinutes(1), Duration.ofMinutes(15));

		for (int tentativa = 0; tentativa < 5; tentativa++) {
			service.registrarFalha(" Usuario.Teste ");
		}

		assertThatThrownBy(() -> service.verificar("usuario.teste")).isInstanceOf(LockedException.class);

		service.registrarSucesso("USUARIO.TESTE");
		assertThatCode(() -> service.verificar("usuario.teste")).doesNotThrowAnyException();
	}

}
