package com.aclg.apecan.paciente.dto;

import java.util.List;

public record PacienteDetalheResultado(PacienteDetalheDto paciente, List<HistoricoStatusPacienteDto> historico) {
}
