package com.aclg.apecan.equipamento.dto;

public interface ResumoCategoriaEquipamento {

	Long getIdCategoria();

	String getCategoria();

	Long getQuantidadeTotal();

	Long getQuantidadeAtiva();

	Long getQuantidadeInativa();

	Long getQuantidadeEmprestada();

}
