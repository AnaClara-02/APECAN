package com.aclg.apecan.backup.service;

import com.aclg.apecan.auth.security.UsuarioPrincipal;
import com.aclg.apecan.backup.config.BackupProperties;
import com.aclg.apecan.usuario.entity.*;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Executar somente com um PostgreSQL descartável exclusivo: -Dapecan.test.postgres.url=... */
@EnabledIf("bancoDisponivel")
@SpringBootTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
class BackupSnapshotIntegrationTests {
    private static org.testcontainers.postgresql.PostgreSQLContainer container;
    static boolean bancoDisponivel() {
        String url = System.getProperty("apecan.test.postgres.url");
        return url != null ? url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/apecan_audit")
                : org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
    }
    @DynamicPropertySource static void banco(DynamicPropertyRegistry r) {
        if (System.getProperty("apecan.test.postgres.url") == null) {
            container = new org.testcontainers.postgresql.PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("apecan_audit").withUsername("apecan_audit")
                .withPassword(java.util.UUID.randomUUID().toString());
            container.start();
            r.add("spring.datasource.url", container::getJdbcUrl);
            r.add("spring.datasource.username", container::getUsername);
            r.add("spring.datasource.password", container::getPassword);
        } else {
            r.add("spring.datasource.url", () -> System.getProperty("apecan.test.postgres.url"));
            r.add("spring.datasource.username", () -> "apecan_audit");
            r.add("spring.datasource.password", () -> "");
        }
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
    @AfterAll static void encerrarContainer() { if (container != null) container.stop(); }
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired BackupSqlService backup;
    @Autowired BackupProperties properties;
    @MockitoSpyBean VersoesBackupService versoes;
    @TempDir Path temporario;

    @Test void snapshotExcluiEscritaConcorrenteERestauraArquivoValido() throws Exception {
        properties.setDiretorioTemporario(temporario);
        var usuario = new Usuario("Administrador Teste", "admin.snapshot", "52998224725",
            "snapshot@example.invalid", "/images/usuario-padrao.svg", "5514999999999", TipoPerfil.ADMINISTRADOR, null);
        String senha = "Senha ficticia snapshot 2026";
        usuario.definirSenhaDefinitiva(encoder.encode(senha), LocalDateTime.now());
        usuario = usuarios.saveAndFlush(usuario);
        var principal = UsuarioPrincipal.de(usuario);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        inserirPaciente("Paciente anterior", "11111111111", usuario.getId());
        Long usuarioId = usuario.getId();
        AtomicBoolean primeira = new AtomicBoolean(true);
        // Executado entre COUNT(*) e o primeiro SELECT de dados, dentro da transação real.
        doAnswer(chamada -> {
            if (primeira.compareAndSet(true, false)) {
                assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class)).isEqualTo("repeatable read");
                assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class)).isEqualTo("on");
                try (var executor = Executors.newSingleThreadExecutor()) {
                    executor.submit(() -> inserirPaciente("Paciente concorrente", "22222222222", usuarioId)).get(10, TimeUnit.SECONDS);
                }
            }
            return chamada.callRealMethod();
        }).when(versoes).versaoEsquemaAtual();
        BackupArquivo arquivo = null;
        try {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class)).isEqualTo(12);
            arquivo = backup.exportar(senha);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pacientes", Integer.class)).isEqualTo(2);
            String texto = Files.readString(arquivo.caminho());
            assertThat(texto).contains("-- registros:1");
            var analise = backup.analisar(new MockMultipartFile("arquivo", "backup.sql", "text/plain", Files.readAllBytes(arquivo.caminho())));
            backup.importar(analise.token(), senha);
            assertThat(jdbc.queryForList("SELECT nome FROM pacientes", String.class)).containsExactly("Paciente anterior");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM historico_backups WHERE resultado='SUCESSO'", Integer.class)).isEqualTo(2);
            properties.setTamanhoMaximo(org.springframework.util.unit.DataSize.ofBytes(100));
            assertThatThrownBy(() -> backup.exportar(senha)).isInstanceOf(com.aclg.apecan.shared.exception.OperacaoInvalidaException.class);
        } finally {
            backup.limpar(arquivo);
            SecurityContextHolder.clearContext();
        }
        try (var arquivos = Files.list(temporario)) { assertThat(arquivos.toList()).isEmpty(); }
    }

    private void inserirPaciente(String nome, String cpf, Long usuarioId) {
        jdbc.update("""
            INSERT INTO pacientes (nome,cpf,data_nascimento,telefone,endereco,local_tratamento,status,criado_por_usuario_id)
            VALUES (?,?,DATE '2000-01-01','5514999999999','Endereço fictício','Local fictício','ATIVO',?)
            """, nome, cpf, usuarioId);
    }
}
