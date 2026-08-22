package com.aclg.apecan.shared.validation;

import com.aclg.apecan.shared.exception.OperacaoInvalidaException;

public final class CpfNormalizer {

    private CpfNormalizer() {
    }

    public static String normalizar(String cpf) {
        if (cpf == null) {
            throw new OperacaoInvalidaException("CPF_OBRIGATORIO", "O CPF é obrigatório.");
        }

        String somenteDigitos = cpf.replaceAll("\\D", "");
        if (somenteDigitos.length() != 11) {
            throw new OperacaoInvalidaException(
                "CPF_FORMATO_INVALIDO",
                "O CPF deve possuir 11 dígitos."
            );
        }
        return somenteDigitos;
    }
}
