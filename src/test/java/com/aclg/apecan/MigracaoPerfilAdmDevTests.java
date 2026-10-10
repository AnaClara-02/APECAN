package com.aclg.apecan;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class MigracaoPerfilAdmDevTests {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
        .withDatabaseName("apecan_perfis")
        .withUsername("teste")
        .withPassword("senha-descartavel");

    @Test
    void V13ConverteAdministradoresSemTrocarContaSenhaOuHistoricoAnterior() {
        var dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("12")).load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Long id = jdbc.queryForObject("""
            INSERT INTO usuarios (
                nome, senha_hash, login, cpf, email, foto_url, telefone,
                tipo_de_perfil, status, primeiro_acesso_pendente, senha_definitiva_em
            ) VALUES (
                'Administrador de migração', '{bcrypt}hash-ficticio', 'migracao.admin',
                '00000000000', 'migracao@example.invalid', '/images/usuario-padrao.svg',
                '5511999999999', 'ADMINISTRADOR', 'ATIVO', FALSE, CURRENT_TIMESTAMP
            ) RETURNING id_usuario
            """, Long.class);

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT tipo_de_perfil FROM usuarios WHERE id_usuario = ?", String.class, id))
            .isEqualTo("ADM_DEV");
        assertThat(jdbc.queryForObject("SELECT senha_hash FROM usuarios WHERE id_usuario = ?", String.class, id))
            .isEqualTo("{bcrypt}hash-ficticio");
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM historico_administracao_usuarios
             WHERE id_usuario = ? AND realizado_por_usuario_id IS NULL
               AND tipo_evento = 'CONVERSAO_PERFIL_ADM_DEV'
               AND perfil_anterior = 'ADMINISTRADOR' AND perfil_novo = 'ADM_DEV'
            """, Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_exclusoes_teste", Integer.class)).isZero();
    }
}
