package com.aclg.apecan.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class RedefinirSenhaForm {

	@NotBlank
	private String token;

	@NotBlank(message = "Informe a nova senha.")
	private String senha;

	@NotBlank(message = "Confirme a nova senha.")
	private String confirmacaoSenha;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public String getConfirmacaoSenha() {
		return confirmacaoSenha;
	}

	public void setConfirmacaoSenha(String confirmacaoSenha) {
		this.confirmacaoSenha = confirmacaoSenha;
	}

}
