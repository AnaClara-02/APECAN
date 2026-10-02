package com.aclg.apecan.auth.service;
import java.time.LocalDateTime;

/** Finalizado pelo callback sincrono apos commit, antes do retorno ao controller. */
public final class AtivacaoEmitida {
    private final LocalDateTime expiraEm;
    private volatile String mensagem;
    private final String linkLocal;
    public AtivacaoEmitida(LocalDateTime expiraEm, String mensagem, String linkLocal) {
        this.expiraEm = expiraEm;
        this.mensagem = mensagem;
        this.linkLocal = linkLocal;
    }
    public LocalDateTime expiraEm() { return expiraEm; }
    public String mensagem() { return mensagem; }
    public String linkLocal() { return linkLocal; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public String getMensagem() { return mensagem; }
    public String getLinkLocal() { return linkLocal; }
    public void envioAceito() { mensagem = "Envio aceito pelo provedor. Confira a caixa de entrada e o spam."; }
    public void envioFalhou() {
        mensagem = "Conta salva, mas o envio não foi confirmado. Consulte o usuário e reemita a ativação.";
    }
}
