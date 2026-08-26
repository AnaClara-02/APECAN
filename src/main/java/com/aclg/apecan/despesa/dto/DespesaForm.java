package com.aclg.apecan.despesa.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class DespesaForm {

	@NotBlank
	@Size(max = 50)
	private String tipo;

	@NotBlank
	@Size(max = 255)
	private String descricao;

	@NotBlank
	@Size(max = 20)
	private String numeroNotaFiscal;

	@NotNull
	@DecimalMin("0.01")
	private BigDecimal valor;

	@NotNull
	@PastOrPresent
	private LocalDate data;

	@NotBlank
	@Size(max = 150)
	private String destino;

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String v) {
		tipo = v;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String v) {
		descricao = v;
	}

	public String getNumeroNotaFiscal() {
		return numeroNotaFiscal;
	}

	public void setNumeroNotaFiscal(String v) {
		numeroNotaFiscal = v;
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

}
