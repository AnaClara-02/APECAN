package com.aclg.apecan.relatorio.entity;

import com.aclg.apecan.usuario.entity.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "historico_exportacoes")
public class HistoricoExportacao {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_exportacao")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_relatorio", nullable = false, length = 40)
	private TipoRelatorio tipo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private FormatoRelatorio formato;

	@Column(length = 500)
	private String filtros;

	@Column(name = "quantidade_registros", nullable = false)
	private int quantidadeRegistros;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "exportado_por_usuario_id", nullable = false,
			foreignKey = @ForeignKey(name = "fk_historico_exportacoes_usuario"))
	private Usuario exportadoPor;

	@Column(name = "exportado_em", nullable = false)
	private LocalDateTime exportadoEm;

	protected HistoricoExportacao() {}

	public HistoricoExportacao(TipoRelatorio tipo, FormatoRelatorio formato, String filtros,
			int quantidadeRegistros, Usuario exportadoPor, LocalDateTime exportadoEm) {
		this.tipo = Objects.requireNonNull(tipo);
		this.formato = Objects.requireNonNull(formato);
		this.filtros = filtros;
		this.quantidadeRegistros = quantidadeRegistros;
		this.exportadoPor = Objects.requireNonNull(exportadoPor);
		this.exportadoEm = Objects.requireNonNull(exportadoEm);
	}
}
