package com.aclg.apecan.backup.service;

import java.time.LocalDateTime;

public record ResumoDadosAtuais(long tamanho, long quantidadeRegistros, LocalDateTime ultimaAlteracao) {
	public boolean possuiDados() { return quantidadeRegistros > 0; }
	public String tamanhoFormatado() { return ResumoArquivoSql.formatar(tamanho); }
}
