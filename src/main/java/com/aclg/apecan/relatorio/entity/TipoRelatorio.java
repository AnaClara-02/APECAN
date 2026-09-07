package com.aclg.apecan.relatorio.entity;

public enum TipoRelatorio {
	PACIENTES("pacientes"),
	VOLUNTARIOS("voluntarios"),
	EMPRESTIMOS("emprestimos"),
	DOACOES("doacoes"),
	MOVIMENTACOES_FINANCEIRAS("movimentacoes-financeiras");

	private final String slug;

	TipoRelatorio(String slug) {
		this.slug = slug;
	}

	public String getSlug() {
		return slug;
	}

	public static TipoRelatorio deSlug(String valor) {
		for (TipoRelatorio tipo : values()) {
			if (tipo.slug.equalsIgnoreCase(valor) || tipo.name().equalsIgnoreCase(valor.replace('-', '_'))) {
				return tipo;
			}
		}
		throw new IllegalArgumentException("Tipo de relatorio invalido.");
	}
}
