package com.aclg.apecan.relatorio.controller;

import com.aclg.apecan.relatorio.service.*;
import com.aclg.apecan.relatorio.entity.*;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RelatorioDownloadTests {
    @TempDir Path diretorio;
    @Test void falhaNoDownloadTambemApagaArquivoELiberaVaga() throws Exception {
        Path path = Files.writeString(diretorio.resolve("relatorio.pdf"), "%PDF ficticio");
        Runnable liberar = mock(Runnable.class);
        var arquivo = new RelatorioArquivo(path, "application/pdf", "relatorio.pdf", liberar);
        var exportacao = mock(RelatorioExportacaoService.class);
        when(exportacao.exportar(TipoRelatorio.PACIENTES, FormatoRelatorio.PDF, null, null)).thenReturn(arquivo);
        var response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenThrow(new IOException("Conexão interrompida"));
        var controller = new RelatorioController(mock(RelatorioService.class), exportacao);
        assertThatThrownBy(() -> controller.exportar("pacientes", FormatoRelatorio.PDF, null, null, response))
                .isInstanceOf(IOException.class);
        assertThat(path).doesNotExist();
        arquivo.close();
        verify(liberar, times(1)).run();
    }
}
