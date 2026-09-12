package com.aclg.apecan.backup.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record BackupManifest(
		int formato,
		String versaoAplicacao,
		String versaoEsquema,
		String versaoPostgreSql,
		LocalDateTime criadoEm,
		String fusoHorario,
		Map<String, String> checksums,
		List<String> componentes) {
}
