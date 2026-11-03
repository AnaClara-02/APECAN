package com.aclg.apecan.shared.validation;

import java.util.Locale;

public final class CnpjValidator {
	private static final int[] PESOS_PRIMEIRO_DV = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
	private static final int[] PESOS_SEGUNDO_DV = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

	private CnpjValidator() {
	}

	public static String normalizar(String valor) {
		if (valor == null) return null;
		return valor.trim().toUpperCase(Locale.ROOT).replaceAll("[.\\-/\\s]", "");
	}

	public static boolean valido(String valor) {
		String cnpj = normalizar(valor);
		if (cnpj == null || !cnpj.matches("[A-Z0-9]{12}[0-9]{2}")) return false;
		if (cnpj.chars().distinct().count() == 1) return false;

		int primeiro = calcularDigito(cnpj, PESOS_PRIMEIRO_DV, 12);
		if (cnpj.charAt(12) - '0' != primeiro) return false;
		int segundo = calcularDigito(cnpj, PESOS_SEGUNDO_DV, 13);
		return cnpj.charAt(13) - '0' == segundo;
	}

	private static int calcularDigito(String cnpj, int[] pesos, int quantidade) {
		int soma = 0;
		for (int i = 0; i < quantidade; i++) {
			// A regra da Receita usa o valor ASCII do caractere menos 48.
			soma += (cnpj.charAt(i) - '0') * pesos[i];
		}
		int resto = soma % 11;
		return resto < 2 ? 0 : 11 - resto;
	}
}
