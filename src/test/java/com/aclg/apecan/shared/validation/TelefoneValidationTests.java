package com.aclg.apecan.shared.validation;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelefoneValidationTests {

	@Test
	void deveNormalizarEFormatarCelular() {
		assertThat(TelefoneNormalizer.normalizar("+55 (14) 99876-5432")).isEqualTo("5514998765432");
		assertThat(TelefoneFormatter.formatar("14998765432")).isEqualTo("+55 (14) 99876-5432");
	}

	@Test
	void deveNormalizarEFormatarTelefoneComOitoDigitos() {
		assertThat(TelefoneNormalizer.normalizar("(14) 3456-7890")).isEqualTo("551434567890");
		assertThat(TelefoneFormatter.formatar("551434567890")).isEqualTo("+55 (14) 3456-7890");
	}

	@Test
	void deveRejeitarTelefoneSemDddOuComCaracteresInvalidos() {
		assertThatThrownBy(() -> TelefoneNormalizer.normalizar("99876-5432"))
			.isInstanceOf(OperacaoInvalidaException.class);
		assertThatThrownBy(() -> TelefoneNormalizer.normalizar("telefone 14 99876-5432"))
			.isInstanceOf(OperacaoInvalidaException.class);
	}

}
