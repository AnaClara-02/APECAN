package com.aclg.apecan.backup.entity;

import com.aclg.apecan.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_backups")
public class HistoricoBackup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_backup")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OperacaoBackup operacao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ResultadoBackup resultado;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private OrigemBackup origem;

	@Column(name = "versao_backup", length = 50)
	private String versaoBackup;

	@Column(length = 64)
	private String checksum;

	@Column(name = "codigo_falha", length = 60)
	private String codigoFalha;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "responsavel_usuario_id", foreignKey = @ForeignKey(name = "fk_historico_backups_usuario"))
	private Usuario responsavel;

	@Column(name = "realizado_em", nullable = false, updatable = false)
	private LocalDateTime realizadoEm;

	protected HistoricoBackup() {
	}

	public HistoricoBackup(OperacaoBackup operacao, ResultadoBackup resultado, OrigemBackup origem,
			String versaoBackup, String checksum, String codigoFalha, Usuario responsavel, LocalDateTime realizadoEm) {
		this.operacao = operacao;
		this.resultado = resultado;
		this.origem = origem;
		this.versaoBackup = versaoBackup;
		this.checksum = checksum;
		this.codigoFalha = codigoFalha;
		this.responsavel = responsavel;
		this.realizadoEm = realizadoEm;
	}

	public Long getId() { return id; }
	public OperacaoBackup getOperacao() { return operacao; }
	public ResultadoBackup getResultado() { return resultado; }
	public OrigemBackup getOrigem() { return origem; }
	public String getVersaoBackup() { return versaoBackup; }
	public String getChecksum() { return checksum; }
	public String getCodigoFalha() { return codigoFalha; }
	public Usuario getResponsavel() { return responsavel; }
	public LocalDateTime getRealizadoEm() { return realizadoEm; }
}
