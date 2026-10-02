package com.aclg.apecan.backup.service;
import com.aclg.apecan.backup.config.BackupProperties;
import com.aclg.apecan.auth.security.SessaoUsuarioService;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class RecuperacaoReversaoTests {
    @TempDir Path temp;
    private Cenario cenario() throws Exception {
        Path trabalho=Files.createDirectory(temp.resolve("trabalho"));
        Path extraido=Files.createDirectory(temp.resolve("extraido"));
        Path dump=Files.writeString(extraido.resolve("database.dump"),"dump-ficticio");
        var props=new BackupProperties();
        var crypto=mock(BackupCriptografiaService.class);
        var pg=mock(PostgreSqlBackupClient.class);
        var versoes=mock(VersoesBackupService.class);
        var usuarios=mock(UsuarioRepository.class);
        var flyway=mock(Flyway.class);
        var jdbc=mock(JdbcTemplate.class);
        var sessoes=mock(SessaoUsuarioService.class);
        @SuppressWarnings("unchecked") ObjectProvider<Flyway> provider=mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(flyway);
        when(crypto.criarDiretorioTemporario(anyString())).thenReturn(trabalho);
        when(crypto.abrir(any(),any())).thenReturn(new BackupExtraido(extraido,dump,
            new BackupManifest(1,"teste","10","18",LocalDateTime.now(),"America/Sao_Paulo",Map.of(),List.of())));
        when(versoes.suporta("10")).thenReturn(true);
        doAnswer(inv -> { Files.writeString(inv.getArgument(0),"estado-anterior-ficticio"); return null; })
            .when(pg).exportar(any());
        var service=new RecuperacaoService(props,crypto,pg,versoes,mock(BackupAuditoriaService.class),usuarios,
            jdbc,provider,sessoes,mock(RecuperacaoEncerramentoService.class),mock(ChecksumService.class),Clock.systemUTC());
        return new Cenario(service,crypto,pg,flyway,jdbc,sessoes,trabalho);
    }
    private MockMultipartFile upload() {
        return new MockMultipartFile("arquivo","backup.apecan-backup","application/octet-stream",new byte[]{1,2,3});
    }
    @Test void falhaNaReversaoPreservaCopiaTecnica() throws Exception {
        var c=cenario();
        when(c.flyway.migrate()).thenThrow(new IllegalStateException("migracao-falhou"));
        doThrow(new IllegalStateException("reversao-falhou")).when(c.pg).restaurar(c.trabalho.resolve("estado-anterior.dump"));
        assertThatThrownBy(() -> c.service.restaurar(upload(),"senha-ficticia-longa"))
            .isInstanceOf(OperacaoInvalidaException.class).hasMessageContaining("preservada");
        assertThat(c.trabalho.resolve("estado-anterior.dump")).exists();
    }
    @Test void sucessoInvalidaTokensSessoesELimpaTemporarios() throws Exception {
        var c=cenario();
        c.service.restaurar(upload(),"senha-ficticia-longa");
        verify(c.flyway).migrate();
        verify(c.jdbc).update(contains("UPDATE tokens_credencial"));
        verify(c.sessoes).encerrarTodasSessoes();
        assertThat(c.trabalho).doesNotExist();
    }
    @Test void arquivoInvalidoNaoExecutaDumpNemRestore() throws Exception {
        var c=cenario();
        when(c.crypto.abrir(any(),any())).thenThrow(new OperacaoInvalidaException("BACKUP_INVALIDO","Arquivo inválido."));
        assertThatThrownBy(() -> c.service.restaurar(upload(),"senha-ficticia-longa"))
            .isInstanceOf(OperacaoInvalidaException.class);
        verifyNoInteractions(c.pg);
        assertThat(c.trabalho).doesNotExist();
    }
    private record Cenario(RecuperacaoService service, BackupCriptografiaService crypto,
        PostgreSqlBackupClient pg, Flyway flyway, JdbcTemplate jdbc, SessaoUsuarioService sessoes, Path trabalho) {}
}
