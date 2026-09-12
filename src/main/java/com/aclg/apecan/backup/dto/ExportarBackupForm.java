package com.aclg.apecan.backup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExportarBackupForm {

	@NotBlank(message = "Informe sua senha atual.")
	private String senhaAtual;

	@NotBlank(message = "Informe uma senha para proteger o backup.")
	@Size(min = 14, max = 128, message = "A senha do backup deve ter entre 14 e 128 caracteres.")
	private String senhaBackup;

	@NotBlank(message = "Confirme a senha do backup.")
	private String confirmacaoSenhaBackup;

	public String getSenhaAtual() { return senhaAtual; }
	public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
	public String getSenhaBackup() { return senhaBackup; }
	public void setSenhaBackup(String senhaBackup) { this.senhaBackup = senhaBackup; }
	public String getConfirmacaoSenhaBackup() { return confirmacaoSenhaBackup; }
	public void setConfirmacaoSenhaBackup(String confirmacaoSenhaBackup) { this.confirmacaoSenhaBackup = confirmacaoSenhaBackup; }
}
