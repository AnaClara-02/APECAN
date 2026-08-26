package com.aclg.apecan.paciente.dto;

import com.aclg.apecan.paciente.entity.StatusPaciente;

import java.time.LocalDateTime;

public record HistoricoStatusPacienteDto(StatusPaciente status, LocalDateTime alteradoEm, String alteradoPor,
		String observacao) {
}
