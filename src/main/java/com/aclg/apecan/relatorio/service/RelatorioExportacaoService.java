package com.aclg.apecan.relatorio.service;

import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.doacao.entity.Doacao;
import com.aclg.apecan.doacao.repository.DoacaoRepository;
import com.aclg.apecan.emprestimo.entity.EmprestimoEquipamento;
import com.aclg.apecan.emprestimo.repository.EmprestimoEquipamentoRepository;
import com.aclg.apecan.equipamento.repository.EquipamentoRepository;
import com.aclg.apecan.financeiro.entity.MovimentacaoFinanceira;
import com.aclg.apecan.financeiro.repository.MovimentacaoFinanceiraRepository;
import com.aclg.apecan.paciente.entity.Paciente;
import com.aclg.apecan.paciente.repository.PacienteRepository;
import com.aclg.apecan.relatorio.entity.*;
import com.aclg.apecan.relatorio.repository.HistoricoExportacaoRepository;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.shared.validation.CpfFormatter;
import com.aclg.apecan.shared.validation.TelefoneFormatter;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.voluntario.entity.Voluntario;
import com.aclg.apecan.voluntario.repository.VoluntarioRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class RelatorioExportacaoService {
	private static final int LOTE = 500;
	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter ARQUIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

	private final PacienteRepository pacientes;
	private final VoluntarioRepository voluntarios;
	private final EmprestimoEquipamentoRepository emprestimos;
	private final DoacaoRepository doacoes;
	private final MovimentacaoFinanceiraRepository movimentos;
	private final EquipamentoRepository equipamentos;
	private final HistoricoExportacaoRepository historico;
	private final UsuarioRepository usuarios;
	private final UsuarioAtual usuarioAtual;
	private final Clock clock;

	public RelatorioExportacaoService(PacienteRepository pacientes, VoluntarioRepository voluntarios,
			EmprestimoEquipamentoRepository emprestimos, DoacaoRepository doacoes,
			MovimentacaoFinanceiraRepository movimentos, EquipamentoRepository equipamentos,
			HistoricoExportacaoRepository historico, UsuarioRepository usuarios, UsuarioAtual usuarioAtual, Clock clock) {
		this.pacientes = pacientes;
		this.voluntarios = voluntarios;
		this.emprestimos = emprestimos;
		this.doacoes = doacoes;
		this.movimentos = movimentos;
		this.equipamentos = equipamentos;
		this.historico = historico;
		this.usuarios = usuarios;
		this.usuarioAtual = usuarioAtual;
		this.clock = clock;
	}

	@Transactional
	public RelatorioArquivo exportar(TipoRelatorio tipo, FormatoRelatorio formato, LocalDate inicio, LocalDate fim) {
		validarPeriodo(inicio, fim);
		Tabela tabela = carregar(tipo, inicio, fim);
		byte[] conteudo = formato == FormatoRelatorio.PDF ? pdf(tipo, tabela, inicio, fim) : xlsx(tipo, tabela, inicio, fim);
		Usuario responsavel = usuarios.findById(usuarioAtual.exigirId()).orElseThrow();
		String filtros = inicio == null && fim == null ? "sem periodo"
				: "inicio=" + Objects.toString(inicio, "") + ";fim=" + Objects.toString(fim, "");
		historico.save(new HistoricoExportacao(tipo, formato, filtros, tabela.linhas().size(), responsavel,
				LocalDateTime.now(clock)));
		String nome = "relatorio-" + tipo.name().toLowerCase(Locale.ROOT).replace('_', '-') + "-"
				+ LocalDateTime.now(clock).format(ARQUIVO) + "." + formato.getExtensao();
		return new RelatorioArquivo(conteudo, formato.getMimeType(), nome);
	}

	private Tabela carregar(TipoRelatorio tipo, LocalDate inicio, LocalDate fim) {
		return switch (tipo) {
			case PACIENTES -> pacientes();
			case VOLUNTARIOS -> voluntarios();
			case EMPRESTIMOS -> emprestimos(inicio, fim);
			case DOACOES -> doacoes(inicio, fim);
			case MOVIMENTACOES_FINANCEIRAS -> movimentos(inicio, fim);
		};
	}

	private Tabela pacientes() {
		List<String[]> linhas = new ArrayList<>();
		int pagina = 0;
		Page<Paciente> lote;
		do {
			lote = pacientes.pesquisar("", "", null, PageRequest.of(pagina++, LOTE, Sort.by("nome")));
			for (Paciente p : lote)
				linhas.add(new String[]{s(p.getId()), p.getNome(), CpfFormatter.mascarar(p.getCpf()), data(p.getDataNascimento()),
						TelefoneFormatter.formatar(p.getTelefone()), p.getLocalTratamento(), s(p.getStatus())});
		} while (lote.hasNext());
		return new Tabela(new String[]{"ID", "Nome", "CPF", "Nascimento", "Telefone", "Tratamento", "Status"}, linhas);
	}

	private Tabela voluntarios() {
		List<String[]> linhas = new ArrayList<>();
		int pagina = 0;
		Page<Voluntario> lote;
		do {
			lote = voluntarios.pesquisar("", null, PageRequest.of(pagina++, LOTE, Sort.by("nome")));
			for (Voluntario v : lote)
				linhas.add(new String[]{s(v.getId()), v.getNome(), CpfFormatter.mascarar(v.getCpf()), data(v.getDataNascimento()),
						TelefoneFormatter.formatar(v.getTelefone()), s(v.getStatus())});
		} while (lote.hasNext());
		return new Tabela(new String[]{"ID", "Nome", "CPF", "Nascimento", "Telefone", "Status"}, linhas);
	}

	private Tabela emprestimos(LocalDate inicio, LocalDate fim) {
		List<String[]> linhas = new ArrayList<>();
		int pagina = 0;
		Page<EmprestimoEquipamento> lote;
		do {
			lote = emprestimos.pesquisarRelatorio(inicio, fim,
					PageRequest.of(pagina++, LOTE, Sort.by("dataEmprestimo").descending()));
			for (EmprestimoEquipamento e : lote)
				linhas.add(new String[]{s(e.getId()), "Equipamento #" + e.getEquipamento().getId(),
						e.getEquipamento().getCategoria().getNome(), e.getPaciente().getNome(), data(e.getDataEmprestimo()),
						data(e.getDataPrevistaDevolucao()), data(e.getDataDevolucao()),
						e.estaAberto() ? "ATIVO" : "INATIVO"});
		} while (lote.hasNext());
		return new Tabela(new String[]{"ID", "Equipamento", "Categoria", "Paciente", "Empréstimo", "Previsão", "Devolução", "Status"}, linhas);
	}

	private Tabela doacoes(LocalDate inicio, LocalDate fim) {
		List<String[]> linhas = new ArrayList<>();
		int pagina = 0;
		Page<Doacao> lote;
		do {
			lote = doacoes.pesquisar(null, inicio, fim, PageRequest.of(pagina++, LOTE, Sort.by("dataDoacao").descending()));
			List<Long> ids = lote.getContent().stream().map(Doacao::getId).toList();
			Map<Long, MovimentacaoFinanceira> porDoacao = ids.isEmpty() ? Map.of()
				: movimentos.findAllByDoacaoIdIn(ids).stream().collect(java.util.stream.Collectors.toMap(
					m -> m.getDoacao().getId(), m -> m));
			Map<Long, Long> quantidades = ids.isEmpty() ? Map.of()
				: equipamentos.contarPorDoacoes(ids).stream().collect(java.util.stream.Collectors.toMap(
					q -> q.getDoacaoId(), q -> q.getQuantidade()));
			for (Doacao d : lote) {
				BigDecimal valor = porDoacao.containsKey(d.getId()) ? porDoacao.get(d.getId()).getValor() : null;
				linhas.add(new String[]{s(d.getId()), data(d.getDataDoacao()), s(d.getTipo()), d.getFonteDoacao(),
						s(d.getQuantidade()), s(d.getUnidade()), s(quantidades.getOrDefault(d.getId(), 0L)), moeda(valor)});
			}
		} while (lote.hasNext());
		return new Tabela(new String[]{"ID", "Data", "Tipo", "Fonte", "Quantidade", "Unidade", "Equipamentos", "Valor"}, linhas);
	}

	private Tabela movimentos(LocalDate inicio, LocalDate fim) {
		List<String[]> linhas = new ArrayList<>();
		int pagina = 0;
		Page<MovimentacaoFinanceira> lote;
		do {
			lote = movimentos.pesquisar(null, inicio, fim,
					PageRequest.of(pagina++, LOTE, Sort.by("dataMovimentacao").descending()));
			for (MovimentacaoFinanceira m : lote)
				linhas.add(new String[]{s(m.getId()), data(m.getDataMovimentacao()), s(m.getTipo()), moeda(m.getValor()),
						m.getDestino(), s(m.getOrigem()), m.getOrigemDescricao(),
						m.getDoacao() == null ? "" : s(m.getDoacao().getId())});
		} while (lote.hasNext());
		return new Tabela(new String[]{"ID", "Data", "Tipo", "Valor", "Destino", "Origem", "Descrição", "Doação"}, linhas);
	}

	private byte[] xlsx(TipoRelatorio tipo, Tabela tabela, LocalDate inicio, LocalDate fim) {
		try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
			Sheet planilha = workbook.createSheet("Relatório");
			CellStyle cabecalho = workbook.createCellStyle();
			Font negrito = workbook.createFont(); negrito.setBold(true); cabecalho.setFont(negrito);
			Row titulo = planilha.createRow(0); titulo.createCell(0).setCellValue("APECAN — " + titulo(tipo));
			Row filtro = planilha.createRow(1); filtro.createCell(0).setCellValue(periodo(inicio, fim));
			Row headers = planilha.createRow(3);
			for (int i = 0; i < tabela.cabecalhos().length; i++) { Cell c = headers.createCell(i); c.setCellValue(tabela.cabecalhos()[i]); c.setCellStyle(cabecalho); planilha.setColumnWidth(i, 4800); }
			int numero = 4;
			for (String[] linha : tabela.linhas()) {
				Row row = planilha.createRow(numero++);
				for (int i = 0; i < linha.length; i++) row.createCell(i).setCellValue(seguroExcel(linha[i]));
			}
			workbook.write(saida);
			workbook.dispose();
			return saida.toByteArray();
		} catch (Exception e) { throw new IllegalStateException("Nao foi possivel gerar o arquivo XLSX.", e); }
	}

	private byte[] pdf(TipoRelatorio tipo, Tabela tabela, LocalDate inicio, LocalDate fim) {
		try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
			Document documento = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
			PdfWriter.getInstance(documento, saida);
			documento.open();
			byte[] fonteBytes = new ClassPathResource("static/fonts/nunito-sans-variable.ttf").getInputStream().readAllBytes();
			BaseFont base = BaseFont.createFont("nunito-sans-variable.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fonteBytes, null);
			org.openpdf.text.Font fonte = new org.openpdf.text.Font(base, 8);
			org.openpdf.text.Font tituloFonte = new org.openpdf.text.Font(base, 16, org.openpdf.text.Font.BOLD);
			documento.add(new Paragraph("APECAN — " + titulo(tipo), tituloFonte));
			documento.add(new Paragraph(periodo(inicio, fim), fonte));
			documento.add(Chunk.NEWLINE);
			PdfPTable tabelaPdf = new PdfPTable(tabela.cabecalhos().length); tabelaPdf.setWidthPercentage(100); tabelaPdf.setHeaderRows(1);
			for (String h : tabela.cabecalhos()) { PdfPCell c = new PdfPCell(new Phrase(h, fonte)); c.setBackgroundColor(new java.awt.Color(233, 228, 245)); tabelaPdf.addCell(c); }
			for (String[] linha : tabela.linhas()) for (String valor : linha) tabelaPdf.addCell(new Phrase(valor, fonte));
			documento.add(tabelaPdf); documento.close();
			return saida.toByteArray();
		} catch (Exception e) { throw new IllegalStateException("Nao foi possivel gerar o arquivo PDF.", e); }
	}

	private void validarPeriodo(LocalDate inicio, LocalDate fim) {
		if (inicio != null && fim != null && inicio.isAfter(fim))
			throw new OperacaoInvalidaException("PERIODO_INVALIDO", "A data inicial nao pode ser posterior a data final.");
	}

	private String periodo(LocalDate inicio, LocalDate fim) {
		return inicio == null && fim == null ? "Todos os períodos" : "Período: " + data(inicio) + " a " + data(fim);
	}
	private String titulo(TipoRelatorio tipo) { return tipo.name().replace('_', ' '); }
	private String data(LocalDate valor) { return valor == null ? "" : valor.format(DATA); }
	private String moeda(BigDecimal valor) { return valor == null ? "" : "R$ " + valor.setScale(2).toString().replace('.', ','); }
	private String s(Object valor) { return valor == null ? "" : valor.toString(); }
	private String seguroExcel(String valor) { return valor != null && !valor.isEmpty() && "=+-@".indexOf(valor.charAt(0)) >= 0 ? "'" + valor : valor; }
	private record Tabela(String[] cabecalhos, List<String[]> linhas) {}
}
