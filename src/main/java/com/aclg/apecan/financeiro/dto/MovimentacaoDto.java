package com.aclg.apecan.financeiro.dto;

import com.aclg.apecan.financeiro.entity.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimentacaoDto(Long id, TipoMovimentacao tipo, BigDecimal valor, LocalDate data, String destino,
		OrigemMovimentacao origem, String descricao, Long doacaoId) {
}
