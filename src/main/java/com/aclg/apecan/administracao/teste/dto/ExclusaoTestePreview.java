package com.aclg.apecan.administracao.teste.dto;

import com.aclg.apecan.administracao.teste.entity.TipoRegistroExclusaoTeste;

public record ExclusaoTestePreview(
    TipoRegistroExclusaoTeste tipo,
    Long id,
    String nome,
    String detalhes,
    boolean podeExcluir,
    String bloqueio,
    String impacto
) {
}
