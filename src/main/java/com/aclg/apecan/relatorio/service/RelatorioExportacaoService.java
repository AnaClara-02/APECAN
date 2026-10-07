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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import jakarta.persistence.EntityManager;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import com.aclg.apecan.shared.io.SaidaLimitada;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
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
	private final EntityManager entityManager;
	private final LimitesExportacao limites;
	private final TransactionTemplate leitura;
	// Inclui a transmissão: um cliente lento não pode acumular arquivos temporários.
	private final Semaphore vaga = new Semaphore(1);

	public RelatorioExportacaoService(PacienteRepository pacientes, VoluntarioRepository voluntarios,
			EmprestimoEquipamentoRepository emprestimos, DoacaoRepository doacoes,
			MovimentacaoFinanceiraRepository movimentos, EquipamentoRepository equipamentos,
			HistoricoExportacaoRepository historico, UsuarioRepository usuarios, UsuarioAtual usuarioAtual, Clock clock,
            EntityManager entityManager, LimitesExportacao limites, PlatformTransactionManager manager) {
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
        this.entityManager = entityManager;
        this.limites = limites;
        this.leitura = new TransactionTemplate(manager);
        this.leitura.setReadOnly(true);
        this.leitura.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.leitura.setTimeout(limites.segundos());
	}

	public RelatorioArquivo exportar(TipoRelatorio tipo, FormatoRelatorio formato, LocalDate inicio, LocalDate fim) {
		validarPeriodo(inicio, fim);
		if (!vaga.tryAcquire()) throw new OperacaoInvalidaException("EXPORTACAO_EM_ANDAMENTO",
                "Há outra exportação em andamento. Aguarde a conclusão e tente novamente.");
        Path arquivo = null;
        try {
            arquivo = Files.createTempFile("apecan-relatorio-", "." + formato.getExtensao());
            Path destino = arquivo;
            long prazo = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(limites.segundos());
            Integer quantidade = leitura.execute(status -> {
                try (OutputStream saida = new SaidaLimitada(Files.newOutputStream(destino), limites.bytes())) {
                    return formato == FormatoRelatorio.PDF ? pdf(tipo, saida, inicio, fim, prazo)
                            : xlsx(tipo, saida, inicio, fim, prazo);
                } catch (com.aclg.apecan.shared.exception.RegraNegocioException e) { throw e; }
                  catch (Exception e) { throw new IllegalStateException("Não foi possível gerar o relatório.", e); }
            });
            Usuario responsavel = usuarios.findById(usuarioAtual.exigirId()).orElseThrow();
            String filtros = inicio == null && fim == null ? "sem periodo"
                    : "inicio=" + Objects.toString(inicio, "") + ";fim=" + Objects.toString(fim, "");
            historico.save(new HistoricoExportacao(tipo, formato, filtros, quantidade, responsavel, LocalDateTime.now(clock)));
            String nome = "relatorio-" + tipo.name().toLowerCase(Locale.ROOT).replace('_', '-') + "-"
                    + LocalDateTime.now(clock).format(ARQUIVO) + "." + formato.getExtensao();
            return new RelatorioArquivo(arquivo, formato.getMimeType(), nome, vaga::release);
        } catch (Exception e) {
            if (arquivo != null) try { Files.deleteIfExists(arquivo); } catch (java.io.IOException limpeza) { e.addSuppressed(limpeza); }
            vaga.release();
            if (e instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Não foi possível gerar o relatório.", e);
        }
	}

    private int carregar(TipoRelatorio tipo, LocalDate inicio, LocalDate fim, Consumer<String[]> destino, long prazo) {
        int[] quantidade = {0};
        Consumer<String[]> linha = valores -> {
            verificarPrazo(prazo);
            if (++quantidade[0] > limites.registros()) throw new OperacaoInvalidaException("RELATORIO_MUITO_GRANDE",
                    "O relatório excede o limite de registros. Reduza o período ou solicite uma exportação administrativa.");
            destino.accept(valores);
        };
        switch (tipo) {
            case PACIENTES -> pacientes(linha);
            case VOLUNTARIOS -> voluntarios(linha);
            case EMPRESTIMOS -> emprestimos(inicio, fim, linha);
            case DOACOES -> doacoes(inicio, fim, linha);
            case MOVIMENTACOES_FINANCEIRAS -> movimentos(inicio, fim, linha);
        }
        verificarPrazo(prazo);
        return quantidade[0];
    }

    private void verificarPrazo(long prazo) {
        if (System.nanoTime() > prazo || Thread.currentThread().isInterrupted())
            throw new OperacaoInvalidaException("EXPORTACAO_EXPIRADA", "A exportação excedeu o tempo permitido. Reduza o período.");
    }

	private void pacientes(Consumer<String[]> destino) {
		int pagina = 0;
		Page<Paciente> lote;
		do {
			lote = pacientes.pesquisar("", "", null, PageRequest.of(pagina++, LOTE, Sort.by("nome", "id")));
			for (Paciente p : lote)
				destino.accept(new String[]{s(p.getId()), p.getNome(), CpfFormatter.mascarar(p.getCpf()), data(p.getDataNascimento()),
						TelefoneFormatter.formatar(p.getTelefone()), p.getLocalTratamento(), s(p.getStatus())});
			entityManager.clear();
		} while (lote.hasNext());

	}

	private void voluntarios(Consumer<String[]> destino) {
		int pagina = 0;
		Page<Voluntario> lote;
		do {
			lote = voluntarios.pesquisar("", null, PageRequest.of(pagina++, LOTE, Sort.by("nome", "id")));
			for (Voluntario v : lote)
				destino.accept(new String[]{s(v.getId()), v.getNome(), CpfFormatter.mascarar(v.getCpf()), data(v.getDataNascimento()),
						TelefoneFormatter.formatar(v.getTelefone()), s(v.getStatus())});
			entityManager.clear();
		} while (lote.hasNext());

	}

	private void emprestimos(LocalDate inicio, LocalDate fim, Consumer<String[]> destino) {
		int pagina = 0;
		Page<EmprestimoEquipamento> lote;
		do {
			lote = emprestimos.pesquisarRelatorio(inicio, fim,
					PageRequest.of(pagina++, LOTE, Sort.by("dataEmprestimo", "id").descending()));
			for (EmprestimoEquipamento e : lote)
				destino.accept(new String[]{s(e.getId()), "Equipamento #" + e.getEquipamento().getId(),
						e.getEquipamento().getCategoria().getNome(), e.getPaciente().getNome(), data(e.getDataEmprestimo()),
						data(e.getDataPrevistaDevolucao()), data(e.getDataDevolucao()),
						e.estaAberto() ? "ATIVO" : "INATIVO"});
			entityManager.clear();
		} while (lote.hasNext());

	}

	private void doacoes(LocalDate inicio, LocalDate fim, Consumer<String[]> destino) {
		int pagina = 0;
		Page<Doacao> lote;
		do {
			lote = doacoes.pesquisar(null, inicio, fim, PageRequest.of(pagina++, LOTE, Sort.by("dataDoacao", "id").descending()));
			List<Long> ids = lote.getContent().stream().map(Doacao::getId).toList();
			Map<Long, MovimentacaoFinanceira> porDoacao = ids.isEmpty() ? Map.of()
				: movimentos.findAllByDoacaoIdIn(ids).stream().collect(java.util.stream.Collectors.toMap(
					m -> m.getDoacao().getId(), m -> m));
			Map<Long, Long> quantidades = ids.isEmpty() ? Map.of()
				: equipamentos.contarPorDoacoes(ids).stream().collect(java.util.stream.Collectors.toMap(
					q -> q.getDoacaoId(), q -> q.getQuantidade()));
			for (Doacao d : lote) {
				BigDecimal valor = porDoacao.containsKey(d.getId()) ? porDoacao.get(d.getId()).getValor() : null;
				destino.accept(new String[]{s(d.getId()), data(d.getDataDoacao()), s(d.getTipo()), d.getFonteDoacao(),
						s(d.getQuantidade()), s(d.getUnidade()), s(quantidades.getOrDefault(d.getId(), 0L)), moeda(valor)});
			}
			entityManager.clear();
		} while (lote.hasNext());

	}

	private void movimentos(LocalDate inicio, LocalDate fim, Consumer<String[]> destino) {
		int pagina = 0;
		Page<MovimentacaoFinanceira> lote;
		do {
			lote = movimentos.pesquisar(null, inicio, fim,
					PageRequest.of(pagina++, LOTE, Sort.by("dataMovimentacao", "id").descending()));
			for (MovimentacaoFinanceira m : lote)
				destino.accept(new String[]{s(m.getId()), data(m.getDataMovimentacao()), s(m.getTipo()), moeda(m.getValor()),
						m.getDestino(), s(m.getOrigem()), m.getOrigemDescricao(),
						m.getDoacao() == null ? "" : s(m.getDoacao().getId())});
			entityManager.clear();
		} while (lote.hasNext());

	}

	@SuppressWarnings("deprecation") // POI 5.5.1: descartar antes de close também quando carregar falha.
    private int xlsx(TipoRelatorio tipo, OutputStream saida, LocalDate inicio, LocalDate fim, long prazo) throws Exception {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
                AutoCloseable temporarios = () -> workbook.dispose()) {
            workbook.setCompressTempFiles(true);
            Sheet planilha = workbook.createSheet("Relatório");
            CellStyle cabecalho = workbook.createCellStyle();
            Font negrito = workbook.createFont(); negrito.setBold(true); cabecalho.setFont(negrito);
            planilha.createRow(0).createCell(0).setCellValue("APECAN — " + titulo(tipo));
            planilha.createRow(1).createCell(0).setCellValue(periodo(inicio, fim));
            Row headers = planilha.createRow(3);
            String[] nomes = cabecalhos(tipo);
            for (int i = 0; i < nomes.length; i++) {
                Cell c = headers.createCell(i); c.setCellValue(nomes[i]); c.setCellStyle(cabecalho); planilha.setColumnWidth(i, 4800);
            }
            int[] numero = {4};
            int quantidade = carregar(tipo, inicio, fim, linha -> {
                Row row = planilha.createRow(numero[0]++);
                for (int i = 0; i < linha.length; i++) row.createCell(i).setCellValue(seguroExcel(linha[i]));
            }, prazo);
            workbook.write(saida);
            verificarPrazo(prazo);
            return quantidade;
        }
    }

    private int pdf(TipoRelatorio tipo, OutputStream saida, LocalDate inicio, LocalDate fim, long prazo) throws Exception {
        Document documento = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        try {
            PdfWriter.getInstance(documento, saida);
            documento.open();
            byte[] fonteBytes;
            try (var entrada = new ClassPathResource("static/fonts/nunito-sans-variable.ttf").getInputStream()) {
                fonteBytes = entrada.readAllBytes();
            }
            BaseFont base = BaseFont.createFont("nunito-sans-variable.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fonteBytes, null);
            org.openpdf.text.Font fonte = new org.openpdf.text.Font(base, 8);
            documento.add(new Paragraph("APECAN — " + titulo(tipo), new org.openpdf.text.Font(base, 16, org.openpdf.text.Font.BOLD)));
            documento.add(new Paragraph(periodo(inicio, fim), fonte));
            documento.add(Chunk.NEWLINE);
            String[] nomes = cabecalhos(tipo);
            PdfPTable tabela = new PdfPTable(nomes.length);
            tabela.setWidthPercentage(100); tabela.setHeaderRows(1); tabela.setComplete(false);
            for (String h : nomes) {
                PdfPCell c = new PdfPCell(new Phrase(h, fonte)); c.setBackgroundColor(new java.awt.Color(233, 228, 245)); tabela.addCell(c);
            }
            int[] lote = {0};
            int quantidade = carregar(tipo, inicio, fim, linha -> {
                for (String valor : linha) tabela.addCell(new Phrase(valor, fonte));
                if (++lote[0] == 100) {
                    documento.add(tabela); tabela.flushContent(); lote[0] = 0;
                }
            }, prazo);
            tabela.setComplete(true); documento.add(tabela);
            verificarPrazo(prazo);
            return quantidade;
        } finally { documento.close(); }
    }

    private String[] cabecalhos(TipoRelatorio tipo) {
        return switch (tipo) {
            case PACIENTES -> new String[]{"ID", "Nome", "CPF", "Nascimento", "Telefone", "Tratamento", "Status"};
            case VOLUNTARIOS -> new String[]{"ID", "Nome", "CPF", "Nascimento", "Telefone", "Status"};
            case EMPRESTIMOS -> new String[]{"ID", "Equipamento", "Categoria", "Paciente", "Empréstimo", "Previsão", "Devolução", "Status"};
            case DOACOES -> new String[]{"ID", "Data", "Tipo", "Fonte", "Quantidade", "Unidade", "Equipamentos", "Valor"};
            case MOVIMENTACOES_FINANCEIRAS -> new String[]{"ID", "Data", "Tipo", "Valor", "Destino", "Origem", "Descrição", "Doação"};
        };
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
}
