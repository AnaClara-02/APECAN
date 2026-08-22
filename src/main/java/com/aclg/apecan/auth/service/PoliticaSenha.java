package com.aclg.apecan.auth.service;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PoliticaSenha {

    private static final int MINIMO_CARACTERES = 12;
    private static final int MAXIMO_CARACTERES = 64;
    private static final int MAXIMO_BYTES_BCRYPT = 72;

    public void validar(String senha, String confirmacao) {
        if (senha == null || senha.isBlank()) {
            throw new OperacaoInvalidaException(
                "SENHA_OBRIGATORIA",
                "Informe a senha."
            );
        }

        int caracteres = senha.codePointCount(0, senha.length());
        if (caracteres < MINIMO_CARACTERES || caracteres > MAXIMO_CARACTERES) {
            throw new OperacaoInvalidaException(
                "TAMANHO_SENHA_INVALIDO",
                "A senha deve possuir entre 12 e 64 caracteres."
            );
        }

        if (senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_BCRYPT) {
            throw new OperacaoInvalidaException(
                "SENHA_MUITO_LONGA",
                "A senha ultrapassa o limite seguro de 72 bytes."
            );
        }

        if (!senha.equals(confirmacao)) {
            throw new OperacaoInvalidaException(
                "CONFIRMACAO_SENHA_DIFERENTE",
                "A confirmacao da senha nao corresponde."
            );
        }
    }
}
