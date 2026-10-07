package com.aclg.apecan.relatorio.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/** O consumidor deve fechar o arquivo mesmo quando o download for interrompido. */
public final class RelatorioArquivo implements AutoCloseable {
    private final Path caminho;
    private final String mimeType;
    private final String nomeArquivo;
    private final Runnable liberar;
    private final AtomicBoolean fechado = new AtomicBoolean();
    public RelatorioArquivo(Path caminho, String mimeType, String nomeArquivo, Runnable liberar) {
        this.caminho = caminho; this.mimeType = mimeType; this.nomeArquivo = nomeArquivo; this.liberar = liberar;
    }
    public Path caminho() { return caminho; }
    public String mimeType() { return mimeType; }
    public String nomeArquivo() { return nomeArquivo; }
    @Override public void close() throws IOException {
        if (fechado.compareAndSet(false, true)) {
            try { Files.deleteIfExists(caminho); } finally { liberar.run(); }
        }
    }
}
