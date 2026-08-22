package com.aclg.apecan.shared.exception;

import java.util.Objects;

public abstract class RegraNegocioException extends RuntimeException {

    private final String codigo;

    protected RegraNegocioException(String codigo, String mensagem) {
        super(Objects.requireNonNull(mensagem));
        this.codigo = Objects.requireNonNull(codigo);
    }

    public String getCodigo() {
        return codigo;
    }
}
