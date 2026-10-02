package com.aclg.apecan.backup.dto;

import jakarta.validation.constraints.NotBlank;

public class ExportarBackupSqlForm {
	@NotBlank(message = "Informe sua senha atual.")
	private String senhaAtual;
	public String getSenhaAtual() { return senhaAtual; }
	public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
}
