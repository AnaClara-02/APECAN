package com.aclg.apecan.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SolicitarRedefinicaoForm {

	@NotBlank(message = "Informe o e-mail.")
	@Email(message = "Informe um e-mail valido.")
	@Size(max = 254)
	private String email;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
