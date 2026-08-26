package com.aclg.apecan.equipamento.entity;

import com.aclg.apecan.doacao.entity.Doacao;
import com.aclg.apecan.shared.audit.EntidadeAuditavel;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
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
import jakarta.persistence.Version;

import java.util.Objects;

@Entity
@Table(name = "equipamentos")
public class Equipamento extends EntidadeAuditavel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_equipamento")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_categoria", nullable = false, foreignKey = @ForeignKey(name = "fk_equipamentos_categoria"))
	private CategoriaEquipamento categoria;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_doacao", foreignKey = @ForeignKey(name = "fk_equipamentos_doacao"))
	private Doacao doacao;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_conservacao", nullable = false, length = 15)
	private EstadoConservacao estadoConservacao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private StatusEquipamento status = StatusEquipamento.ATIVO;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criado_por_usuario_id", nullable = false,
			foreignKey = @ForeignKey(name = "fk_equipamentos_criado_por"))
	private Usuario criadoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atualizado_por_usuario_id", foreignKey = @ForeignKey(name = "fk_equipamentos_atualizado_por"))
	private Usuario atualizadoPor;

	@Version
	@Column(nullable = false)
	private long versao;

	protected Equipamento() {
	}

	public Equipamento(CategoriaEquipamento categoria, Doacao doacao, EstadoConservacao estadoConservacao,
			Usuario criadoPor) {
		this.categoria = Objects.requireNonNull(categoria);
		this.doacao = doacao;
		this.estadoConservacao = Objects.requireNonNull(estadoConservacao);
		this.criadoPor = Objects.requireNonNull(criadoPor);
	}

	public void atualizarCategoria(CategoriaEquipamento categoria, Usuario responsavel) {
		this.categoria = Objects.requireNonNull(categoria);
		this.atualizadoPor = Objects.requireNonNull(responsavel);
	}

	public void atualizarEstadoConservacao(EstadoConservacao estado, Usuario responsavel) {
		this.estadoConservacao = Objects.requireNonNull(estado);
		this.atualizadoPor = Objects.requireNonNull(responsavel);
	}

	public void emprestar(Usuario responsavel) {
		if (status != StatusEquipamento.ATIVO) {
			throw new OperacaoInvalidaException("EQUIPAMENTO_INDISPONIVEL",
					"Somente equipamento ativo pode ser emprestado.");
		}
		status = StatusEquipamento.EMPRESTADO;
		atualizadoPor = Objects.requireNonNull(responsavel);
	}

	public void registrarDevolucao(EstadoConservacao estado, Usuario responsavel) {
		if (status != StatusEquipamento.EMPRESTADO) {
			throw new OperacaoInvalidaException("EQUIPAMENTO_NAO_EMPRESTADO", "O equipamento não está emprestado.");
		}
		status = StatusEquipamento.ATIVO;
		estadoConservacao = Objects.requireNonNull(estado);
		atualizadoPor = Objects.requireNonNull(responsavel);
	}

	public void desativar(Usuario responsavel) {
		if (status == StatusEquipamento.EMPRESTADO) {
			throw new OperacaoInvalidaException("EQUIPAMENTO_EMPRESTADO",
					"Equipamento emprestado não pode ser desativado.");
		}
		status = StatusEquipamento.INATIVO;
		atualizadoPor = Objects.requireNonNull(responsavel);
	}

	public void reativar(Usuario responsavel) {
		if (status == StatusEquipamento.EMPRESTADO) {
			throw new OperacaoInvalidaException("EQUIPAMENTO_EMPRESTADO",
					"Equipamento emprestado não pode ser reativado.");
		}
		status = StatusEquipamento.ATIVO;
		atualizadoPor = Objects.requireNonNull(responsavel);
	}

	public Long getId() {
		return id;
	}

	public CategoriaEquipamento getCategoria() {
		return categoria;
	}

	public Doacao getDoacao() {
		return doacao;
	}

	public EstadoConservacao getEstadoConservacao() {
		return estadoConservacao;
	}

	public StatusEquipamento getStatus() {
		return status;
	}

	public Usuario getCriadoPor() {
		return criadoPor;
	}

	public Usuario getAtualizadoPor() {
		return atualizadoPor;
	}

	public long getVersao() {
		return versao;
	}

}
