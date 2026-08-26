package com.aclg.apecan.emprestimo.dto;

import java.time.LocalDate;

public record EmprestimoDto(Long id, Long equipamentoId, String categoria, Long pacienteId, String paciente,
		LocalDate dataEmprestimo, LocalDate dataPrevistaDevolucao, LocalDate dataDevolucao, boolean atrasado,
		String observacao) {
}
