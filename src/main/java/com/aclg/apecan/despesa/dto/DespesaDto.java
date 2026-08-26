package com.aclg.apecan.despesa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaDto(Long id, String tipo, String descricao, String numeroNotaFiscal, BigDecimal valor,
		LocalDate data, String destino) {
}
