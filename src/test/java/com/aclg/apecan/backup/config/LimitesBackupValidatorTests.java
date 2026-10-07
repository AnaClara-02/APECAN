package com.aclg.apecan.backup.config;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import static org.assertj.core.api.Assertions.*;
class LimitesBackupValidatorTests {
    @Test void recusaExportacaoMaiorQueUploadEOuSemEspacoMultipart() {
        var p = new BackupProperties(); p.setTamanhoMaximo(DataSize.ofGigabytes(1));
        assertThatThrownBy(() -> new LimitesBackupValidator(p, DataSize.ofMegabytes(5), DataSize.ofMegabytes(6)))
                .isInstanceOf(IllegalStateException.class);
        p.setTamanhoMaximo(DataSize.ofMegabytes(5));
        assertThatThrownBy(() -> new LimitesBackupValidator(p, DataSize.ofMegabytes(5), DataSize.ofMegabytes(5)))
                .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> new LimitesBackupValidator(p, DataSize.ofMegabytes(5), DataSize.ofMegabytes(6)))
                .doesNotThrowAnyException();
    }
}
