package com.aclg.apecan.emprestimo.dto;

import com.aclg.apecan.equipamento.entity.EstadoConservacao;
import java.time.LocalDate;

public record HistoricoEquipamentoDto(Long id, Long pacienteId, String paciente, LocalDate dataEmprestimo,
		LocalDate dataPrevistaDevolucao, LocalDate dataDevolucao, EstadoConservacao estadoConservacaoDevolucao,
		String situacao) {
}
