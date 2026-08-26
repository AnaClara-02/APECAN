package com.aclg.apecan.equipamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoriaEquipamentoForm {

	@NotBlank
	@Size(max = 80)
	private String nome;

	@Size(max = 255)
	private String descricao;

	public String getNome() {
		return nome;
	}

	public void setNome(String v) {
		nome = v;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String v) {
		descricao = v;
	}

}
