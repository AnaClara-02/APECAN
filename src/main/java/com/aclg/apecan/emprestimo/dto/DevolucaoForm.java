package com.aclg.apecan.emprestimo.dto;

import com.aclg.apecan.equipamento.entity.EstadoConservacao;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class DevolucaoForm {

	@NotNull
	@PastOrPresent
	private LocalDate dataDevolucao;

	@NotNull
	private EstadoConservacao estadoConservacao;

	private boolean inativarEquipamento;

	public LocalDate getDataDevolucao() {
		return dataDevolucao;
	}

	public void setDataDevolucao(LocalDate v) {
		dataDevolucao = v;
	}

	public EstadoConservacao getEstadoConservacao() {
		return estadoConservacao;
	}

	public void setEstadoConservacao(EstadoConservacao v) {
		estadoConservacao = v;
	}

	public boolean isInativarEquipamento() {
		return inativarEquipamento;
	}

	public void setInativarEquipamento(boolean v) {
		inativarEquipamento = v;
	}

}
