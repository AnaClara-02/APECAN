package com.aclg.apecan.backup.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/** Impede produzir backups que o upload da mesma instalação não aceita. */
@Component
public class LimitesBackupValidator {
    public LimitesBackupValidator(BackupProperties backup,
            @Value("${spring.servlet.multipart.max-file-size:5MB}") DataSize arquivo,
            @Value("${spring.servlet.multipart.max-request-size:6MB}") DataSize requisicao) {
        long limite = backup.getTamanhoMaximo().toBytes();
        if (limite <= 0 || arquivo.toBytes() < limite
                || requisicao.toBytes() < limite + DataSize.ofMegabytes(1).toBytes()) {
            throw new IllegalStateException("O limite de backup deve caber no upload; a requisição precisa de 1 MB adicional para o multipart.");
        }
    }
}
