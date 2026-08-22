package com.aclg.apecan.shared.exception;

public class ConflitoNegocioException extends RegraNegocioException {

    public ConflitoNegocioException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
