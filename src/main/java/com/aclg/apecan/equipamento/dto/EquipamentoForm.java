package com.aclg.apecan.equipamento.dto;

import com.aclg.apecan.equipamento.entity.EstadoConservacao;
import jakarta.validation.constraints.NotNull;

public class EquipamentoForm {

	@NotNull
	private Long categoriaId;

	@NotNull
	private EstadoConservacao estadoConservacao;

	public Long getCategoriaId() {
		return categoriaId;
	}

	public void setCategoriaId(Long v) {
		categoriaId = v;
	}

	public EstadoConservacao getEstadoConservacao() {
		return estadoConservacao;
	}

	public void setEstadoConservacao(EstadoConservacao v) {
		estadoConservacao = v;
	}

}
