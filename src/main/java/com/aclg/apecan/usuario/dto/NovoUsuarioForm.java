package com.aclg.apecan.usuario.dto;

import com.aclg.apecan.shared.validation.CpfValido;
import com.aclg.apecan.shared.validation.TelefoneBrasileiro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class NovoUsuarioForm {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 150, message = "O nome deve possuir no maximo 150 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o login.")
    @Pattern(
        regexp = "^[A-Za-z0-9._-]{3,50}$",
        message = "Use de 3 a 50 letras, numeros, ponto, hifen ou sublinhado."
    )
    private String login;

    @NotBlank(message = "Informe o CPF.")
    @CpfValido
    private String cpf;

    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "Informe um e-mail valido.")
    @Size(max = 254, message = "O e-mail deve possuir no maximo 254 caracteres.")
    private String email;

    @NotBlank(message = "Informe o telefone.")
    @TelefoneBrasileiro
    private String telefone;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
