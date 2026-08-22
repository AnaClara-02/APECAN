package com.aclg.apecan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AtivacaoForm {

    @NotBlank
    private String token;

    @NotBlank(message = "Informe a senha.")
    @Size(
        min = 12,
        max = 64,
        message = "A senha deve possuir entre 12 e 64 caracteres."
    )
    private String senha;

    @NotBlank(message = "Confirme a senha.")
    private String confirmacaoSenha;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getConfirmacaoSenha() { return confirmacaoSenha; }
    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }
}
