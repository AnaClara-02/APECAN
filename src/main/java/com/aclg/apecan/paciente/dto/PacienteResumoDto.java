package com.aclg.apecan.paciente.dto;

import com.aclg.apecan.paciente.entity.StatusPaciente;

import java.time.LocalDate;

public record PacienteResumoDto(Long id, String nome, String cpfMascarado, LocalDate dataNascimento, String telefone,
		String localTratamento, StatusPaciente status) {
}
