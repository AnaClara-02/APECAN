package com.aclg.apecan.equipamento.dto;

import com.aclg.apecan.equipamento.entity.EstadoConservacao;
import com.aclg.apecan.equipamento.entity.StatusEquipamento;

public record EquipamentoDto(Long id, String categoria, EstadoConservacao estadoConservacao, StatusEquipamento status,
		Long doacaoId) {
}
