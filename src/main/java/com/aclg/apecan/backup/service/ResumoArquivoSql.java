package com.aclg.apecan.backup.service;

import java.time.LocalDateTime;

public record ResumoArquivoSql(String nome, long tamanho, LocalDateTime exportadoEm,
		String versaoEsquema, long quantidadeRegistros) {
	public String tamanhoFormatado() { return formatar(tamanho); }
	static String formatar(long bytes) {
		if (bytes < 1024) return bytes + " B";
		if (bytes < 1024L * 1024) return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f KB", bytes / 1024d);
		return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f MB", bytes / (1024d * 1024));
	}
}
