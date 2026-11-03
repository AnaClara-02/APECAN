package com.aclg.apecan.despesa.dto;

import com.aclg.apecan.despesa.entity.BancoPagamentoDespesa;
import com.aclg.apecan.despesa.validation.DadosDespesaValidos;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@DadosDespesaValidos
public class DespesaForm {
	@NotBlank
	@Size(max = 50)
	private String tipo;

	@NotBlank
	@Size(max = 255)
	private String descricao;

	private boolean possuiNotaFiscal;

	@Size(max = 150)
	private String nomeEmitente;

	@Size(max = 18)
	private String cnpjEmitente;

	@Size(max = 20)
	private String numeroNotaFiscal;

	@PastOrPresent
	private LocalDate dataEmissao;

	@NotNull
	@DecimalMin("0.01")
	@Digits(integer = 10, fraction = 2)
	private BigDecimal valor;

	@NotNull
	@PastOrPresent
	private LocalDate dataPagamento;

	@NotBlank
	@Size(max = 150)
	private String destino;

	@NotNull
	private BancoPagamentoDespesa bancoPagamento;

	@Size(max = 150)
	private String outroBancoPagamento;

	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public String getDescricao() { return descricao; }
	public void setDescricao(String descricao) { this.descricao = descricao; }
	public boolean isPossuiNotaFiscal() { return possuiNotaFiscal; }
	public void setPossuiNotaFiscal(boolean possuiNotaFiscal) { this.possuiNotaFiscal = possuiNotaFiscal; }
	public String getNomeEmitente() { return nomeEmitente; }
	public void setNomeEmitente(String nomeEmitente) { this.nomeEmitente = nomeEmitente; }
	public String getCnpjEmitente() { return cnpjEmitente; }
	public void setCnpjEmitente(String cnpjEmitente) { this.cnpjEmitente = cnpjEmitente; }
	public String getNumeroNotaFiscal() { return numeroNotaFiscal; }
	public void setNumeroNotaFiscal(String numeroNotaFiscal) { this.numeroNotaFiscal = numeroNotaFiscal; }
	public LocalDate getDataEmissao() { return dataEmissao; }
	public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }
	public BigDecimal getValor() { return valor; }
	public void setValor(BigDecimal valor) { this.valor = valor; }
	public LocalDate getDataPagamento() { return dataPagamento; }
	public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
	public String getDestino() { return destino; }
	public void setDestino(String destino) { this.destino = destino; }
	public BancoPagamentoDespesa getBancoPagamento() { return bancoPagamento; }
	public void setBancoPagamento(BancoPagamentoDespesa bancoPagamento) { this.bancoPagamento = bancoPagamento; }
	public String getOutroBancoPagamento() { return outroBancoPagamento; }
	public void setOutroBancoPagamento(String outroBancoPagamento) { this.outroBancoPagamento = outroBancoPagamento; }
}
