package com.aclg.apecan.shared.validation;

public final class CpfFormatter {

	private CpfFormatter() {
	}

	public static String formatar(String cpf) {
		String normalizado = CpfNormalizer.normalizar(cpf);
		return normalizado.substring(0, 3) + "." + normalizado.substring(3, 6) + "." + normalizado.substring(6, 9) + "-"
				+ normalizado.substring(9, 11);
	}

	public static String mascarar(String cpf) {
		String normalizado = CpfNormalizer.normalizar(cpf);
		return "***.***.***-" + normalizado.substring(9, 11);
	}

}
