package com.aclg.apecan.financeiro.dto;

import com.aclg.apecan.financeiro.entity.TipoMovimentacao;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class MovimentacaoForm {

	@NotNull
	private TipoMovimentacao tipo;

	@NotNull
	@DecimalMin("0.01")
	private BigDecimal valor;

	@NotNull
	@PastOrPresent
	private LocalDate data;

	@NotBlank
	@Size(max = 150)
	private String destino;

	@NotBlank
	@Size(max = 150)
	private String descricao;

	public TipoMovimentacao getTipo() {
		return tipo;
	}

	public void setTipo(TipoMovimentacao v) {
		tipo = v;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal v) {
		valor = v;
	}

	public LocalDate getData() {
		return data;
	}

	public void setData(LocalDate v) {
		data = v;
	}

	public String getDestino() {
		return destino;
	}

	public void setDestino(String v) {
		destino = v;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String v) {
		descricao = v;
	}

}
