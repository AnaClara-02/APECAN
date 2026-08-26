package com.aclg.apecan.emprestimo.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class EmprestimoForm {

	@NotNull
	private Long equipamentoId;

	@NotNull
	private Long pacienteId;

	@NotNull
	@PastOrPresent
	private LocalDate dataEmprestimo;

	@NotNull
	private LocalDate dataPrevistaDevolucao;

	@Size(max = 255)
	private String observacao;

	public Long getEquipamentoId() {
		return equipamentoId;
	}

	public void setEquipamentoId(Long v) {
		equipamentoId = v;
	}

	public Long getPacienteId() {
		return pacienteId;
	}

	public void setPacienteId(Long v) {
		pacienteId = v;
	}

	public LocalDate getDataEmprestimo() {
		return dataEmprestimo;
	}

	public void setDataEmprestimo(LocalDate v) {
		dataEmprestimo = v;
	}

	public LocalDate getDataPrevistaDevolucao() {
		return dataPrevistaDevolucao;
	}

	public void setDataPrevistaDevolucao(LocalDate v) {
		dataPrevistaDevolucao = v;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String v) {
		observacao = v;
	}

	@AssertTrue(message = "A previsao de devolucao nao pode ser anterior a data do emprestimo.")
	public boolean isPeriodoValido() {
		return dataEmprestimo == null || dataPrevistaDevolucao == null
				|| !dataPrevistaDevolucao.isBefore(dataEmprestimo);
	}

}
