package com.aclg.apecan.paciente.dto;

import com.aclg.apecan.paciente.entity.StatusPaciente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AlterarStatusPacienteForm {

	@NotNull(message = "Selecione o novo status.")
	private StatusPaciente status;

	@NotBlank(message = "Informe uma observacao para a mudanca.")
	@Size(max = 255, message = "A observacao deve possuir no maximo 255 caracteres.")
	private String observacao;

	public StatusPaciente getStatus() {
		return status;
	}

	public void setStatus(StatusPaciente status) {
		this.status = status;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

}
