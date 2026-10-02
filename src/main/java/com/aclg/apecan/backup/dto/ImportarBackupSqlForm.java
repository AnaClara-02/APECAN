package com.aclg.apecan.backup.dto;

import org.springframework.web.multipart.MultipartFile;

public class ImportarBackupSqlForm {
	private MultipartFile arquivo;
	public MultipartFile getArquivo() { return arquivo; }
	public void setArquivo(MultipartFile arquivo) { this.arquivo = arquivo; }
}
