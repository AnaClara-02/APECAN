package com.aclg.apecan.usuario.entity;

public enum TipoPerfil {
    ADM_DEV,
    ADMINISTRADOR,
    USUARIO;

    public String rotulo() {
        return switch (this) {
            case ADM_DEV -> "Adm. Dev.";
            case ADMINISTRADOR -> "Administrador";
            case USUARIO -> "Usuário";
        };
    }
}
