package com.aclg.apecan.voluntario.dto;

import com.aclg.apecan.shared.validation.CpfValido;
import com.aclg.apecan.shared.validation.TelefoneBrasileiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class VoluntarioForm {

	@NotBlank
	@Size(max = 150)
	private String nome;

	@NotBlank
	@CpfValido
	private String cpf;

	@NotNull
	@PastOrPresent
	private LocalDate dataNascimento;

	@NotBlank
	@TelefoneBrasileiro
	private String telefone;

	@NotBlank
	@Size(max = 255)
	private String endereco;

	public String getNome() {
		return nome;
	}

	public void setNome(String v) {
		nome = v;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String v) {
		cpf = v;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate v) {
		dataNascimento = v;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String v) {
		telefone = v;
	}

	public String getEndereco() {
		return endereco;
	}

	public void setEndereco(String v) {
		endereco = v;
	}

}
