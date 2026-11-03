package com.aclg.apecan.despesa.entity;

public enum BancoPagamentoDespesa {
	SICOOB("Banco Cooperativo Sicoob S.A."),
	PAGBANK("PagBank"),
	OUTRO("Outro");

	private final String descricao;

	BancoPagamentoDespesa(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}
}
