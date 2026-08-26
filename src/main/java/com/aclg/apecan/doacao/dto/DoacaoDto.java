package com.aclg.apecan.doacao.dto;

import com.aclg.apecan.doacao.entity.TipoDoacao;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DoacaoDto(Long id, TipoDoacao tipo, LocalDate dataDoacao, String fonteDoacao, BigDecimal quantidade,
		String unidade, Integer equipamentos, BigDecimal valor) {
}
