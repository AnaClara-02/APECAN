package com.aclg.apecan.shared.validation;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;

public final class TelefoneNormalizer {

	private TelefoneNormalizer() {
	}

	public static String normalizar(String telefone) {
		String digitos = telefone == null ? "" : telefone.replaceAll("\\D", "");
		if (digitos.length() < 10 || digitos.length() > 15) {
			throw new OperacaoInvalidaException("TELEFONE_INVALIDO", "O telefone deve possuir entre 10 e 15 digitos.");
		}
		return digitos;
	}

}
