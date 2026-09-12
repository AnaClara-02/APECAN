package com.aclg.apecan.backup.service;

import java.nio.file.Path;

public record BackupArquivo(Path caminho, String nomeArquivo, String checksum) {
}
