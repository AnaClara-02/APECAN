package com.aclg.apecan.backup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class RestaurarBackupForm {

	private MultipartFile arquivo;

	@NotBlank(message = "Informe a senha do arquivo de backup.")
	@Size(max = 128, message = "A senha do backup deve ter no máximo 128 caracteres.")
	private String senhaBackup;

	public MultipartFile getArquivo() { return arquivo; }
	public void setArquivo(MultipartFile arquivo) { this.arquivo = arquivo; }
	public String getSenhaBackup() { return senhaBackup; }
	public void setSenhaBackup(String senhaBackup) { this.senhaBackup = senhaBackup; }
}
