package com.aclg.apecan.shared.exception;

public class RecursoNaoEncontradoException extends RegraNegocioException {

    public RecursoNaoEncontradoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
