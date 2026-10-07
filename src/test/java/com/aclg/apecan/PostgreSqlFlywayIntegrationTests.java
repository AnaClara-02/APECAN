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
	private com.aclg.apecan.backup.service.BackupSqlService backupSqlService;
	@Autowired
	private com.aclg.apecan.usuario.repository.UsuarioRepository usuarioRepository;
	@Autowired
	private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
	@org.junit.jupiter.api.io.TempDir
	java.nio.file.Path temporario;

	@Test
	void deveAplicarMigracoesEValidarRecursosEspecificosDoPostgresql() throws Exception {
		Integer migracoes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success",
				Integer.class);
		Integer views = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.views
				 WHERE table_schema = 'public'
				   AND table_name = 'vw_resumo_equipamentos_por_categoria'
				""", Integer.class);

		assertThat(migracoes).isEqualTo(12);
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
		validarBackupSqlSemContas(usuarioId, pacienteId, senhaTeste);
		validarCicloBackup(usuarioId, equipamentoId);
	}

	private void validarBackupSqlSemContas(Long usuarioId, Long pacienteId, String senha) throws Exception {
		var usuario = usuarioRepository.findById(usuarioId).orElseThrow();
		var principal = com.aclg.apecan.auth.security.UsuarioPrincipal.de(usuario);
		var autenticacao = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
			principal, null, principal.getAuthorities());
		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(autenticacao);
		try {
			var exportado = backupSqlService.exportar(senha);
			byte[] conteudo = java.nio.file.Files.readAllBytes(exportado.caminho());
			String sql = new String(conteudo, java.nio.charset.StandardCharsets.UTF_8);
			assertThat(sql).contains("-- APECAN-SQL-BACKUP:1", "INSERT INTO pacientes");
			assertThat(sql).doesNotContain("teste.integracao", "integracao@example.invalid", "senha-ficticia");
			backupSqlService.limpar(exportado);

			jdbcTemplate.update("UPDATE pacientes SET nome = 'Paciente alterado' WHERE id_paciente = ?", pacienteId);
			var upload = new org.springframework.mock.web.MockMultipartFile("arquivo", "dados.sql",
				"application/sql", conteudo);
			var preparacao = backupSqlService.analisar(upload);
			assertThat(preparacao.dadosAtuais().possuiDados()).isTrue();
			backupSqlService.importar(preparacao.token(), senha);
			assertThat(jdbcTemplate.queryForObject("SELECT nome FROM pacientes WHERE id_paciente = ?", String.class, pacienteId))
				.isEqualTo("Paciente de teste");
			assertThat(jdbcTemplate.queryForObject("SELECT criado_por_usuario_id FROM pacientes WHERE id_paciente = ?", Long.class, pacienteId))
				.isNull();
			assertThat(jdbcTemplate.queryForObject("SELECT criado_por_nome_historico FROM pacientes WHERE id_paciente = ?", String.class, pacienteId))
				.isEqualTo("Usuario de teste");
			assertThat(jdbcTemplate.queryForObject("SELECT importado_em IS NOT NULL FROM pacientes WHERE id_paciente = ?", Boolean.class, pacienteId))
				.isTrue();
			assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuarios WHERE id_usuario = ?", Integer.class, usuarioId))
				.isEqualTo(1);
		}
		finally {
			org.springframework.security.core.context.SecurityContextHolder.clearContext();
		}
	}

	private void validarCicloBackup(Long usuarioId, Long equipamentoId) throws Exception {
		// Tudo ocorre no PostgreSQL descartavel do Testcontainers, nunca no banco operacional.
		var pgDump = POSTGRES.execInContainer("pg_dump", "-U", "apecan_teste", "-d", "apecan_teste",
			"--format=custom", "--no-owner", "--no-privileges", "--file=/tmp/apecan-teste.dump");
		assertThat(pgDump.getExitCode()).isZero();
		var dump = temporario.resolve("database.dump");
		POSTGRES.copyFileFromContainer("/tmp/apecan-teste.dump", dump.toString());
		var props = new com.aclg.apecan.backup.config.BackupProperties();
		props.setDiretorioTemporario(temporario);
		props.setIteracoesPbkdf2(100000);
		var checksum = new com.aclg.apecan.backup.service.ChecksumService();
		var crypto = new com.aclg.apecan.backup.service.BackupCriptografiaService(props,
			tools.jackson.databind.json.JsonMapper.builder().findAndAddModules().build(), checksum);
		var manifesto = new com.aclg.apecan.backup.service.BackupManifest(1,"teste","10","18",
			java.time.LocalDateTime.now(),"America/Sao_Paulo",
			java.util.Map.of("database.dump",checksum.sha256(dump)),java.util.List.of("BANCO_POSTGRESQL"));
		var arquivo = crypto.empacotar(dump,manifesto,"senha-ficticia-de-backup".toCharArray(),temporario);
		assertThat(POSTGRES.execInContainer("createdb","-U","apecan_teste","apecan_restaurado").getExitCode()).isZero();
		try (var extraido = crypto.abrir(arquivo,"senha-ficticia-de-backup".toCharArray())) {
			POSTGRES.copyFileToContainer(org.testcontainers.utility.MountableFile.forHostPath(extraido.dump()),
				"/tmp/restaurar.dump");
			assertThat(POSTGRES.execInContainer("pg_restore","-U","apecan_teste","-d","apecan_restaurado",
				"--clean","--if-exists","--no-owner","--no-privileges","--single-transaction","--exit-on-error",
				"/tmp/restaurar.dump").getExitCode()).isZero();
		}
		var ds=new org.springframework.jdbc.datasource.DriverManagerDataSource(
			POSTGRES.getJdbcUrl().replace("/apecan_teste","/apecan_restaurado"),
			POSTGRES.getUsername(),POSTGRES.getPassword());
		var restaurado=new JdbcTemplate(ds);
		org.flywaydb.core.Flyway.configure().dataSource(ds).load().validate();
		assertThat(restaurado.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success",Integer.class)).isEqualTo(12);
		assertThat(restaurado.queryForObject("SELECT COUNT(*) FROM usuarios WHERE id_usuario=?",Integer.class,usuarioId)).isEqualTo(1);
		assertThat(restaurado.queryForObject("SELECT COUNT(*) FROM emprestimos_equipamentos WHERE id_equipamento=?",
			Integer.class,equipamentoId)).isEqualTo(1);
		assertThat(restaurado.queryForObject("SELECT SUM(quantidade_emprestada) FROM vw_resumo_equipamentos_por_categoria",
			Integer.class)).isEqualTo(1);
		assertThat(restaurado.queryForObject("SELECT nextval(pg_get_serial_sequence('usuarios','id_usuario'))",Long.class))
			.isGreaterThan(usuarioId);
	}

}
