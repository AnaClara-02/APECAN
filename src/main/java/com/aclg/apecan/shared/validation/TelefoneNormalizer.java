package com.aclg.apecan.shared.validation;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;

public final class TelefoneNormalizer {

	private TelefoneNormalizer() {
	}

	public static String normalizar(String telefone) {
		String valor = telefone == null ? "" : telefone.trim();
		if (!valor.matches("[0-9+().\\-\\s]+")) {
			throw invalido();
		}
		String digitos = valor.replaceAll("\\D", "");
		if (digitos.length() == 10 || digitos.length() == 11) {
			digitos = "55" + digitos;
		}
		if ((digitos.length() != 12 && digitos.length() != 13) || !digitos.startsWith("55")) {
			throw invalido();
		}
		return digitos;
	}

	private static OperacaoInvalidaException invalido() {
		return new OperacaoInvalidaException("TELEFONE_INVALIDO",
				"Informe um telefone brasileiro com DDD e 8 ou 9 digitos.");
	}

}
