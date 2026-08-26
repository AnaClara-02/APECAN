package com.aclg.apecan.paciente.dto;

import com.aclg.apecan.paciente.entity.StatusPaciente;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PacienteDetalheDto(Long id, String nome, String cpf, LocalDate dataNascimento, String telefone,
		String endereco, String localTratamento, StatusPaciente status, String criadoPor, String atualizadoPor,
		LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
	public boolean podeAlterarStatus() {
		return status != StatusPaciente.FALECIDO;
	}
}
