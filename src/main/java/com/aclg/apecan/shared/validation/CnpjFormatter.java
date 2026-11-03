package com.aclg.apecan.shared.validation;

public final class CnpjFormatter {
	private CnpjFormatter() {
	}

	public static String formatar(String valor) {
		if (valor == null || valor.isBlank()) return "";
		String cnpj = CnpjValidator.normalizar(valor);
		if (cnpj.length() != 14) return valor;
		return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8)
				+ "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
	}
}
