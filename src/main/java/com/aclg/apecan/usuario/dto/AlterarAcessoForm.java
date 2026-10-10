package com.aclg.apecan.usuario.dto;

import com.aclg.apecan.usuario.entity.TipoPerfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AlterarAcessoForm {

    @NotBlank(message = "Confirme sua senha atual.")
    private String senhaAtual;

    @NotBlank(message = "Informe a justificativa.")
    @Size(
        min = 5,
        max = 500,
        message = "A justificativa deve possuir entre 5 e 500 caracteres."
    )
    private String justificativa;

    private TipoPerfil novoPerfil;

    public String getSenhaAtual() { return senhaAtual; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    public TipoPerfil getNovoPerfil() { return novoPerfil; }
    public void setNovoPerfil(TipoPerfil novoPerfil) { this.novoPerfil = novoPerfil; }
}
