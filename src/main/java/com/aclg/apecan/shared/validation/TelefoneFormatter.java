package com.aclg.apecan.shared.validation;

public final class TelefoneFormatter {

	private TelefoneFormatter() {
	}

	public static String formatar(String telefone) {
		if (telefone == null || telefone.isBlank()) {
			return telefone;
		}
		String digitos = TelefoneNormalizer.normalizar(telefone);
		String ddd = digitos.substring(2, 4);
		String numero = digitos.substring(4);
		int separador = numero.length() == 9 ? 5 : 4;
		return "+55 (" + ddd + ") " + numero.substring(0, separador) + "-" + numero.substring(separador);
	}

}
