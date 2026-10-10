package com.aclg.apecan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = { "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate" })
class PostgreSqlFlywayIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
		.withDatabaseName("apecan_teste")
		.withUsername("apecan_teste")
		.withPassword("senha-descartavel-de-teste");

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private com.aclg.apecan.auth.security.ControleAcessoStore controleStore;
	@Autowired
	private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	@Test
	void deveAplicarMigracoesEValidarRecursosEspecificosDoPostgresql() throws Exception {
		Integer migracoes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success",
				Integer.class);
		Integer views = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.views
				 WHERE table_schema = 'public'
				   AND table_name = 'vw_resumo_equipamentos_por_categoria'
				""", Integer.class);

		assertThat(migracoes).isEqualTo(14);
		assertThat(views).isEqualTo(1);
		Integer tabelaAuditoria = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.tables
				 WHERE table_schema = 'public' AND table_name = 'historico_exportacoes'
				""", Integer.class);
		assertThat(tabelaAuditoria).isEqualTo(1);
		Integer tabelaAuditoriaBackup = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.tables
				 WHERE table_schema = 'public' AND table_name = 'historico_backups'
				""", Integer.class);
		assertThat(tabelaAuditoriaBackup).isEqualTo(1);
		Integer auditoriaExclusoes = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.tables
				 WHERE table_schema = 'public' AND table_name = 'auditoria_exclusoes_teste'
				""", Integer.class);
		assertThat(auditoriaExclusoes).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1", String.class))
			.isEqualTo("14");

		String senhaTeste = "senha-ficticia-integracao";
		Long usuarioId = jdbcTemplate.queryForObject("""
				INSERT INTO usuarios (
				    nome, senha_hash, login, cpf, email, foto_url, telefone,
				    tipo_de_perfil, status, primeiro_acesso_pendente,
				    senha_definitiva_em, criado_em
				) VALUES (
				    'Usuario de teste', ?, 'teste.integracao',
				    '00000000000', 'integracao@example.invalid', '/images/usuario-padrao.svg',
				    '5511999999999', 'ADMINISTRADOR', 'ATIVO', FALSE, CURRENT_TIMESTAMP,
				    CURRENT_TIMESTAMP
				) RETURNING id_usuario
				""", Long.class, passwordEncoder.encode(senhaTeste));
		Long pacienteId = jdbcTemplate.queryForObject("""
				INSERT INTO pacientes (
				    nome, cpf, data_nascimento, telefone, endereco,
				    local_tratamento, status, criado_por_usuario_id
				) VALUES (
				    'Paciente de teste', '11111111111', DATE '2000-01-01',
				    '5511988888888', 'Endereco de teste', 'Local de teste', 'ATIVO', ?
				) RETURNING id_paciente
				""", Long.class, usuarioId);
		Long categoriaId = jdbcTemplate.queryForObject("""
				INSERT INTO categorias_equipamentos (nome, criado_por_usuario_id)
				VALUES ('Categoria de teste', ?) RETURNING id_categoria
				""", Long.class, usuarioId);
		Long equipamentoId = jdbcTemplate.queryForObject("""
				INSERT INTO equipamentos (
				    id_categoria, estado_conservacao, status, criado_por_usuario_id
				) VALUES (?, 'BOM', 'EMPRESTADO', ?) RETURNING id_equipamento
				""", Long.class, categoriaId, usuarioId);

		jdbcTemplate.update("""
				INSERT INTO emprestimos_equipamentos (
				    id_equipamento, id_paciente, data_emprestimo, registrado_por_usuario_id
				) VALUES (?, ?, CURRENT_DATE, ?)
				""", equipamentoId, pacienteId, usuarioId);

		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO emprestimos_equipamentos (
				    id_equipamento, id_paciente, data_emprestimo, registrado_por_usuario_id
				) VALUES (?, ?, CURRENT_DATE, ?)
				""", equipamentoId, pacienteId, usuarioId)).isInstanceOf(DataIntegrityViolationException.class);

		Integer emprestados = jdbcTemplate.queryForObject("""
				SELECT quantidade_emprestada
				  FROM vw_resumo_equipamentos_por_categoria
				 WHERE id_categoria = ?
				""", Integer.class, categoriaId);
		assertThat(emprestados).isEqualTo(1);
		var limite = new com.aclg.apecan.auth.security.LimiteRecuperacaoService(controleStore, java.time.Clock.systemUTC());
		assertThat(limite.permitir("integracao@example.invalid")).isTrue();
		assertThat(limite.permitir("integracao@example.invalid")).isFalse();
	}

}
