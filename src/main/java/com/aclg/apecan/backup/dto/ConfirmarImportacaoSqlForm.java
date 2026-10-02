package com.aclg.apecan.backup.dto;

import jakarta.validation.constraints.NotBlank;

public class ConfirmarImportacaoSqlForm {
	@NotBlank private String token;
	@NotBlank(message = "Informe sua senha atual.") private String senhaAtual;
	public String getToken() { return token; }
	public void setToken(String token) { this.token = token; }
	public String getSenhaAtual() { return senhaAtual; }
	public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
}
