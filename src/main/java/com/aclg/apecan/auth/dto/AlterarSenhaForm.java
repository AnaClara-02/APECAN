package com.aclg.apecan.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AlterarSenhaForm {

	@NotBlank(message = "Informe a senha atual.")
	private String senhaAtual;

	@NotBlank(message = "Informe a nova senha.")
	private String novaSenha;

	@NotBlank(message = "Confirme a nova senha.")
	private String confirmacaoSenha;

	public String getSenhaAtual() {
		return senhaAtual;
	}

	public void setSenhaAtual(String senhaAtual) {
		this.senhaAtual = senhaAtual;
	}

	public String getNovaSenha() {
		return novaSenha;
	}

	public void setNovaSenha(String novaSenha) {
		this.novaSenha = novaSenha;
	}

	public String getConfirmacaoSenha() {
		return confirmacaoSenha;
	}

	public void setConfirmacaoSenha(String confirmacaoSenha) {
		this.confirmacaoSenha = confirmacaoSenha;
	}

}
