package com.aclg.apecan.backup.service;

import com.aclg.apecan.backup.entity.HistoricoBackup;
import com.aclg.apecan.backup.entity.OperacaoBackup;
import com.aclg.apecan.backup.entity.OrigemBackup;
import com.aclg.apecan.backup.entity.ResultadoBackup;
import com.aclg.apecan.backup.repository.HistoricoBackupRepository;
import com.aclg.apecan.usuario.entity.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class BackupAuditoriaService {

	private final HistoricoBackupRepository repository;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	public BackupAuditoriaService(HistoricoBackupRepository repository, JdbcTemplate jdbcTemplate, Clock clock) {
		this.repository = repository;
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	public void registrarExportacao(ResultadoBackup resultado, String versao, String checksum,
			String codigoFalha, Usuario responsavel) {
		repository.save(new HistoricoBackup(OperacaoBackup.EXPORTACAO, resultado,
			OrigemBackup.PAINEL_ADMINISTRATIVO, limitar(versao, 50), checksum, limitar(codigoFalha, 60),
			responsavel, LocalDateTime.now(clock)));
	}

	public void registrarRestauracao(ResultadoBackup resultado, String versao, String checksum, String codigoFalha) {
		jdbcTemplate.update("""
				INSERT INTO historico_backups
				(operacao, resultado, origem, versao_backup, checksum, codigo_falha, realizado_em)
				VALUES ('RESTAURACAO', ?, 'RECUPERACAO_LOCAL', ?, ?, ?, ?)
				""", resultado.name(), limitar(versao, 50), checksum, limitar(codigoFalha, 60), LocalDateTime.now(clock));
	}

	private String limitar(String valor, int tamanho) {
		return valor == null || valor.length() <= tamanho ? valor : valor.substring(0, tamanho);
	}
}
