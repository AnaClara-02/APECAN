package com.aclg.apecan.shared.exception;

public class OperacaoInvalidaException extends RegraNegocioException {

    public OperacaoInvalidaException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
