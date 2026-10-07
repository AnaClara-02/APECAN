package com.aclg.apecan.relatorio.service;

import com.aclg.apecan.auth.security.UsuarioAtual;
import com.aclg.apecan.doacao.repository.DoacaoRepository;
import com.aclg.apecan.emprestimo.repository.EmprestimoEquipamentoRepository;
import com.aclg.apecan.equipamento.repository.EquipamentoRepository;
import com.aclg.apecan.financeiro.repository.MovimentacaoFinanceiraRepository;
import com.aclg.apecan.paciente.entity.Paciente;
import com.aclg.apecan.paciente.repository.PacienteRepository;
import com.aclg.apecan.relatorio.entity.*;
import com.aclg.apecan.relatorio.repository.HistoricoExportacaoRepository;
import com.aclg.apecan.usuario.entity.Usuario;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.voluntario.repository.VoluntarioRepository;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.Clock;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RelatorioExportacaoServiceTests {
    private final PacienteRepository pacientes = mock(PacienteRepository.class);
    private final HistoricoExportacaoRepository historico = mock(HistoricoExportacaoRepository.class);
    private final EntityManager em = mock(EntityManager.class);
    private final Clock clock = Clock.systemUTC();

    private RelatorioExportacaoService servico(LimitesExportacao limites, int quantidade) {
        var usuarios = mock(UsuarioRepository.class);
        var atual = mock(UsuarioAtual.class);
        when(atual.exigirId()).thenReturn(1L);
        when(usuarios.findById(1L)).thenReturn(Optional.of(mock(Usuario.class)));
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(pacientes.pesquisar(anyString(), anyString(), isNull(), any(Pageable.class))).thenAnswer(chamada -> {
            Pageable pagina = chamada.getArgument(3);
            assertThat(pagina.getSort().getOrderFor("id")).isNotNull();
            List<Paciente> linhas = new ArrayList<>();
            for (long i=pagina.getOffset(); i < Math.min(quantidade, pagina.getOffset()+pagina.getPageSize()); i++) {
                var p = mock(Paciente.class);
                when(p.getId()).thenReturn(i+1);
                when(p.getCpf()).thenReturn("52998224725");
                when(p.getTelefone()).thenReturn("14999999999");
                when(p.getNome()).thenReturn(i == quantidade-1 ? "ULTIMA LINHA" : "Paciente " + i);
                linhas.add(p);
            }
            return new PageImpl<>(linhas, pagina, quantidade);
        });
        return new RelatorioExportacaoService(pacientes, mock(VoluntarioRepository.class),
                mock(EmprestimoEquipamentoRepository.class), mock(DoacaoRepository.class),
                mock(MovimentacaoFinanceiraRepository.class), mock(EquipamentoRepository.class), historico,
                usuarios, atual, clock, em, limites, manager);
    }

    @Test void geraTodosOsLotesEmPdfEXlsxELimpaOArquivoDepoisDoUso() throws Exception {
        var service = servico(new LimitesExportacao(2000, 20971520, 60), 1001);
        Path caminho;
        try (var arquivo = service.exportar(TipoRelatorio.PACIENTES, FormatoRelatorio.XLSX, null, null)) {
            caminho = arquivo.caminho();
            try (var entrada = Files.newInputStream(caminho);
                    var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(entrada)) {
                assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(1004);
                assertThat(workbook.getSheetAt(0).getRow(1004).getCell(1).getStringCellValue()).isEqualTo("ULTIMA LINHA");
            }
            assertThatThrownBy(() -> service.exportar(TipoRelatorio.PACIENTES, FormatoRelatorio.PDF, null, null))
                .isInstanceOf(OperacaoInvalidaException.class).hasMessageContaining("andamento");
        }
        assertThat(caminho).doesNotExist();
        try (var arquivo = service.exportar(TipoRelatorio.PACIENTES, FormatoRelatorio.PDF, null, null);
                var reader = new org.openpdf.text.pdf.PdfReader(Files.readAllBytes(arquivo.caminho()))) {
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);
            var extrator = new org.openpdf.text.pdf.parser.PdfTextExtractor(reader);
            assertThat(extrator.getTextFromPage(reader.getNumberOfPages())).contains("ULTIMA LINHA");
        }
        verify(em, times(6)).clear();
        verify(historico, times(2)).save(any());
    }

    @Test void recusaExcessoDeRegistrosSemGravarHistoricoELiberaAVaga() throws Exception {
        var anteriores = temporariosPoi();
        var service = servico(new LimitesExportacao(2, 20971520, 60), 3);
        for (int i=0; i<2; i++) {
            assertThatThrownBy(() -> service.exportar(TipoRelatorio.PACIENTES, FormatoRelatorio.XLSX, null, null))
                .isInstanceOf(OperacaoInvalidaException.class).hasMessageContaining("registros");
        }
        verifyNoInteractions(historico);
        assertThat(temporariosPoi()).isEqualTo(anteriores);
    }

    @Test void recusaArquivoAcimaDoTamanhoELiberaAVaga() {
        var service = servico(new LimitesExportacao(2, 10, 60), 1);
        for (int i=0; i<2; i++) assertThatThrownBy(() -> service.exportar(TipoRelatorio.PACIENTES, FormatoRelatorio.XLSX, null, null))
                .isInstanceOf(RuntimeException.class).hasMessageNotContaining("andamento");
        verifyNoInteractions(historico);
    }

    @Test void prazoEAplicadoMesmoQuandoAConsultaNaoRetornaLinhas() {
        var service = servico(new LimitesExportacao(2, 20971520, 1), 0);
        when(pacientes.pesquisar(anyString(), anyString(), isNull(), any(Pageable.class))).thenAnswer(chamada -> {
            Thread.sleep(1100);
            return Page.empty();
        });
        assertThatThrownBy(() -> service.exportar(TipoRelatorio.PACIENTES, FormatoRelatorio.XLSX, null, null))
            .isInstanceOf(OperacaoInvalidaException.class).hasMessageContaining("tempo");
        verifyNoInteractions(historico);
    }

    private Set<Path> temporariosPoi() throws Exception {
        Path diretorio = Path.of(System.getProperty("java.io.tmpdir"), "poifiles");
        if (!Files.exists(diretorio)) return Set.of();
        try (var arquivos = Files.list(diretorio)) { return arquivos.collect(java.util.stream.Collectors.toSet()); }
    }
}
