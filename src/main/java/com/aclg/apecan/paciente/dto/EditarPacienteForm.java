package com.aclg.apecan.paciente.dto;

import com.aclg.apecan.shared.validation.TelefoneBrasileiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class EditarPacienteForm {

	@NotBlank(message = "Informe o nome.")
	@Size(max = 150, message = "O nome deve possuir no maximo 150 caracteres.")
	private String nome;

	@NotNull(message = "Informe a data de nascimento.")
	@PastOrPresent(message = "A data de nascimento nao pode estar no futuro.")
	private LocalDate dataNascimento;

	@NotBlank(message = "Informe o telefone.")
	@TelefoneBrasileiro
	private String telefone;

	@NotBlank(message = "Informe o endereco.")
	@Size(max = 255, message = "O endereco deve possuir no maximo 255 caracteres.")
	private String endereco;

	@NotBlank(message = "Informe o local de tratamento.")
	@Size(max = 150, message = "O local deve possuir no maximo 150 caracteres.")
	private String localTratamento;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getEndereco() {
		return endereco;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public String getLocalTratamento() {
		return localTratamento;
	}

	public void setLocalTratamento(String localTratamento) {
		this.localTratamento = localTratamento;
	}

}
