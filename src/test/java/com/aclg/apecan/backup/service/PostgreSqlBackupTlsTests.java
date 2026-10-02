package com.aclg.apecan.backup.service;
import com.aclg.apecan.backup.config.BackupProperties;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PostgreSqlBackupTlsTests {
    @Test void preservaTlsDaUrlSemColocarSenhaNaConfiguracaoTls() {
        var client=new PostgreSqlBackupClient(new BackupProperties(),
            "jdbc:postgresql://example.invalid/apecan?sslmode=verify-full&sslrootcert=C%3A%2Fcerts%2Fca.pem","app","ficticia");
        var env=new HashMap<String,String>();
        client.configurarTls(env);
        assertThat(env).containsEntry("PGSSLMODE","verify-full").containsEntry("PGSSLROOTCERT","C:/certs/ca.pem");
        assertThat(env.values()).doesNotContain("ficticia");
    }
    @Test void caDoContainerPodeSerDiferenteDaCaJdbc() {
        var props=new BackupProperties();
        props.setSslMode("verify-full"); props.setSslRootCert("/etc/ssl/certs/ca-certificates.crt");
        var client=new PostgreSqlBackupClient(props,"jdbc:postgresql://example.invalid/apecan?sslmode=verify-full","app","teste");
        var env=new HashMap<String,String>(); client.configurarTls(env);
        assertThat(env).containsEntry("PGSSLROOTCERT","/etc/ssl/certs/ca-certificates.crt");
    }
}
