package com.aclg.apecan.shared.io;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/** Verifica tamanho antes de escrever, sem materializar a saída em memória. */
public class SaidaLimitada extends FilterOutputStream {
    private final long maximo;
    private long escritos;
    public SaidaLimitada(OutputStream destino, long maximo) { super(destino); this.maximo = maximo; }
    private void reservar(int quantidade) {
        if (quantidade > maximo - escritos) throw new OperacaoInvalidaException("ARQUIVO_MUITO_GRANDE",
                "O arquivo excede o limite configurado. Reduza o período ou o volume da exportação.");
        escritos += quantidade;
    }
    @Override public void write(int valor) throws IOException { reservar(1); out.write(valor); }
    @Override public void write(byte[] dados, int inicio, int tamanho) throws IOException {
        reservar(tamanho); out.write(dados, inicio, tamanho);
    }
}
