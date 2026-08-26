package com.aclg.apecan.shared.validation;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TelefoneBrasileiroValidator implements ConstraintValidator<TelefoneBrasileiro, String> {

	@Override
	public boolean isValid(String telefone, ConstraintValidatorContext context) {
		if (telefone == null || telefone.isBlank()) {
			return true;
		}
		try {
			TelefoneNormalizer.normalizar(telefone);
			return true;
		}
		catch (OperacaoInvalidaException exception) {
			return false;
		}
	}

}
