package com.aclg.apecan.backup.service;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;

@Component
public class VersoesBackupService {

	private final JdbcTemplate jdbcTemplate;
	private final ObjectProvider<Flyway> flywayProvider;

	public VersoesBackupService(JdbcTemplate jdbcTemplate, ObjectProvider<Flyway> flywayProvider) {
		this.jdbcTemplate = jdbcTemplate;
		this.flywayProvider = flywayProvider;
	}

	public String versaoAplicacao() {
		String versao = VersoesBackupService.class.getPackage().getImplementationVersion();
		return versao == null || versao.isBlank() ? "desenvolvimento" : versao;
	}

	public String versaoEsquemaAtual() {
		try {
			return jdbcTemplate.queryForObject(
				"SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1",
				String.class);
		}
		catch (RuntimeException exception) {
			return "0";
		}
	}

	public String versaoPostgreSql() {
		try {
			return jdbcTemplate.queryForObject("SHOW server_version", String.class);
		}
		catch (RuntimeException exception) {
			return "desconhecida";
		}
	}

	public boolean suporta(String versao) {
		try {
			return Integer.parseInt(versao) <= maiorVersaoDisponivel();
		}
		catch (NumberFormatException exception) {
			return false;
		}
	}

	public int maiorVersaoDisponivel() {
		Flyway flyway = flywayProvider.getIfAvailable();
		if (flyway == null) {
			return 9;
		}
		return Arrays.stream(flyway.info().all())
			.map(MigrationInfo::getVersion)
			.filter(java.util.Objects::nonNull)
			.map(versao -> versao.getVersion())
			.map(VersoesBackupService::inteiro)
			.max(Comparator.naturalOrder())
			.orElse(9);
	}

	private static int inteiro(String versao) {
		try {
			return Integer.parseInt(versao);
		}
		catch (NumberFormatException exception) {
			return 0;
		}
	}
}
