package com.aclg.apecan.shared.mail;

/** Transporte de mensagens de acesso. Implementacoes nunca devem registrar o conteudo. */
public interface EnvioEmail {
    void enviar(String destino, String assunto, String texto);
}

