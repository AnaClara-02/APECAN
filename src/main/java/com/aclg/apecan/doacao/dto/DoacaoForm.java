package com.aclg.apecan.doacao.dto;

import com.aclg.apecan.doacao.entity.TipoDoacao;
import com.aclg.apecan.equipamento.entity.EstadoConservacao;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class DoacaoForm {

	@NotNull
	private TipoDoacao tipo;

	@NotNull
	@PastOrPresent
	private LocalDate dataDoacao;

	@NotBlank
	@Size(max = 150)
	private String fonteDoacao;

	@DecimalMin("0.01")
	private BigDecimal valor;

	@DecimalMin("0.001")
	private BigDecimal quantidade;

	@Size(max = 30)
	private String unidade;

	private Long categoriaId;

	private EstadoConservacao estadoConservacao;

	@Min(1)
	@Max(100)
	private Integer quantidadeEquipamentos;

	private Long doadorId;

	private Long responsavelId;

	public TipoDoacao getTipo() {
		return tipo;
	}

	public void setTipo(TipoDoacao v) {
		tipo = v;
	}

	public LocalDate getDataDoacao() {
		return dataDoacao;
	}

	public void setDataDoacao(LocalDate v) {
		dataDoacao = v;
	}

	public String getFonteDoacao() {
		return fonteDoacao;
	}

	public void setFonteDoacao(String v) {
		fonteDoacao = v;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal v) {
		valor = v;
	}

	public BigDecimal getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(BigDecimal v) {
		quantidade = v;
	}

	public String getUnidade() {
		return unidade;
	}

	public void setUnidade(String v) {
		unidade = v;
	}

	public Long getCategoriaId() {
		return categoriaId;
	}

	public void setCategoriaId(Long v) {
		categoriaId = v;
	}

	public EstadoConservacao getEstadoConservacao() {
		return estadoConservacao;
	}

	public void setEstadoConservacao(EstadoConservacao v) {
		estadoConservacao = v;
	}

	public Integer getQuantidadeEquipamentos() {
		return quantidadeEquipamentos;
	}

	public void setQuantidadeEquipamentos(Integer v) {
		quantidadeEquipamentos = v;
	}

	public Long getDoadorId() {
		return doadorId;
	}

	public void setDoadorId(Long v) {
		doadorId = v;
	}

	public Long getResponsavelId() {
		return responsavelId;
	}

	public void setResponsavelId(Long v) {
		responsavelId = v;
	}

}
