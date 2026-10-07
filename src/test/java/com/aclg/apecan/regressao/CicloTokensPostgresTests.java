package com.aclg.apecan.regressao;

import com.aclg.apecan.auth.dto.RedefinirSenhaForm;
import com.aclg.apecan.auth.security.UsuarioPrincipal;
import com.aclg.apecan.auth.service.*;
import com.aclg.apecan.shared.exception.OperacaoInvalidaException;
import com.aclg.apecan.usuario.dto.*;
import com.aclg.apecan.usuario.repository.UsuarioRepository;
import com.aclg.apecan.usuario.service.UsuarioService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Banco descartável exclusivo, com transações reais em conexões independentes. */
@EnabledIf("bancoDisponivel")
@SpringBootTest(properties={"spring.flyway.enabled=true","spring.jpa.hibernate.ddl-auto=validate"})
class CicloTokensPostgresTests {
    private static org.testcontainers.postgresql.PostgreSQLContainer container;
    static boolean bancoDisponivel(){
        String url=System.getProperty("apecan.test.tokens.postgres.url");
        return url!=null ? url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/apecan_tokens_test")
            : org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
    }
    @DynamicPropertySource static void banco(DynamicPropertyRegistry props){
        String url=System.getProperty("apecan.test.tokens.postgres.url");
        if(url==null){
            container=new org.testcontainers.postgresql.PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("apecan_tokens_test").withUsername("apecan_audit").withPassword(UUID.randomUUID().toString());
            container.start();
            props.add("spring.datasource.url",container::getJdbcUrl);
            props.add("spring.datasource.username",container::getUsername);
            props.add("spring.datasource.password",container::getPassword);
        }else{
            props.add("spring.datasource.url",()->url);
            props.add("spring.datasource.username",()->"apecan_audit");
            props.add("spring.datasource.password",()->"");
        }
        props.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
    }
    @AfterAll static void encerrarContainer(){if(container!=null)container.stop();}
    @Autowired UsuarioService usuarios;
    @Autowired UsuarioRepository repository;
    @Autowired AtivacaoUsuarioService ativacao;
    @Autowired CredencialService credenciais;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean EntregaRedefinicaoSenha entrega;
    static final String SENHA="Senha ficticia concorrencia 2026";
    Long alvo;
    String convite;
    String email;
    UsuarioPrincipal admin;
    NovoUsuarioForm formulario(String login,String cpf,String endereco){
        var form=new NovoUsuarioForm();form.setNome("Pessoa Concorrencia");form.setLogin(login);
        form.setCpf(cpf);form.setEmail(endereco);form.setTelefone("14999999999");return form;
    }
    String token(String link){return URI.create(link).getRawQuery().substring(6);}
    void autenticar(){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin,null,admin.getAuthorities()));}
    @BeforeEach void preparar(){
        var criado=usuarios.cadastrarPrimeiroAdministrador(formulario("admin.concorrencia","52998224725","admin@example.invalid"));
        ativacao.ativar(token(criado.ativacao().linkLocal()),SENHA,SENHA);
        admin=UsuarioPrincipal.de(repository.findByLogin("admin.concorrencia").orElseThrow());autenticar();
        email=UUID.randomUUID()+"@example.invalid";
        var usuario=usuarios.cadastrar(formulario("alvo.concorrencia","11144477735",email));
        alvo=repository.findByLogin("alvo.concorrencia").orElseThrow().getId();convite=token(usuario.ativacao().linkLocal());
    }
    @AfterEach void limpar(){
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM historico_administracao_usuarios");jdbc.update("DELETE FROM tokens_credencial");
        jdbc.update("DELETE FROM usuarios WHERE login='alvo.concorrencia'");
        jdbc.update("DELETE FROM usuarios WHERE login='admin.concorrencia'");
    }
    void mudarEmail(){var form=usuarios.formularioEdicao(alvo);form.setEmail("novo@example.invalid");usuarios.atualizar(alvo,form);}
    void desativar(){var form=new AlterarAcessoForm();form.setSenhaAtual(SENHA);form.setJustificativa("Teste de concorrencia");usuarios.desativar(alvo,form);}
    String emitirReset(){
        ativacao.ativar(convite,SENHA,SENHA);var capturado=new AtomicReference<String>();
        when(entrega.entregar(any(),anyString())).thenAnswer(i->{capturado.set(i.getArgument(1));return new RedefinicaoEmitida(null);});
        credenciais.solicitar(email);assertThat(capturado.get()).isNotBlank();return capturado.get();
    }
    void redefinir(String token){
        var form=new RedefinirSenhaForm();form.setToken(token);form.setSenha("Outra senha ficticia 2026");form.setConfirmacaoSenha(form.getSenha());
        credenciais.redefinir(form);
    }
    Throwable concorrente(Runnable alteracao,Runnable operacao) throws Exception {
        var bloqueado=new CountDownLatch(1);var liberar=new CountDownLatch(1);var pid=new AtomicInteger();
        var transacao=new TransactionTemplate(manager);transacao.setTimeout(20);
        try(var executor=Executors.newFixedThreadPool(2)){
            var administrador=executor.submit(()->{
                autenticar();
                try{transacao.executeWithoutResult(status->{
                    alteracao.run();bloqueado.countDown();
                    try{if(!liberar.await(15,TimeUnit.SECONDS))throw new IllegalStateException("Timeout aguardando teste");}
                    catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                });}finally{SecurityContextHolder.clearContext();}
            });
            try{
                assertThat(bloqueado.await(10,TimeUnit.SECONDS)).isTrue();
                Future<Throwable> consumidor=executor.submit(()->{
                    try{transacao.executeWithoutResult(status->{pid.set(jdbc.queryForObject("SELECT pg_backend_pid()",Integer.class));operacao.run();});return null;}
                    catch(Throwable falha){return falha;}
                });
                long fim=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);boolean esperou=false;
                while(System.nanoTime()<fim){
                    if(pid.get()!=0 && jdbc.queryForObject("SELECT cardinality(pg_blocking_pids(?))",Integer.class,pid.get())>0){esperou=true;break;}
                    if(consumidor.isDone())break;
                    Thread.sleep(20);
                }
                assertThat(esperou).as("a operação concorrente deve aguardar o lock do usuário").isTrue();
                liberar.countDown();administrador.get(10,TimeUnit.SECONDS);return consumidor.get(10,TimeUnit.SECONDS);
            }finally{liberar.countDown();}
        }
    }
    @Test void trocarEmailSerializaConsumoDoConvite() throws Exception {
        assertThat(concorrente(this::mudarEmail,()->ativacao.ativar(convite,SENHA,SENHA))).isInstanceOf(OperacaoInvalidaException.class);
        assertThat(repository.findById(alvo).orElseThrow().estaAtivado()).isFalse();
    }
    @Test void trocarEmailSerializaConsumoDoReset() throws Exception {
        String token=emitirReset();String hash=repository.findById(alvo).orElseThrow().getSenhaHash();
        assertThat(concorrente(this::mudarEmail,()->redefinir(token))).isInstanceOf(OperacaoInvalidaException.class);
        assertThat(repository.findById(alvo).orElseThrow().getSenhaHash()).isEqualTo(hash);
    }
    @Test void desativarSerializaConsumoDoReset() throws Exception {
        String token=emitirReset();String hash=repository.findById(alvo).orElseThrow().getSenhaHash();
        assertThat(concorrente(this::desativar,()->redefinir(token))).isInstanceOf(OperacaoInvalidaException.class);
        assertThat(repository.findById(alvo).orElseThrow().getSenhaHash()).isEqualTo(hash);
    }
    @Test void trocarEmailImpedeEmissaoDeResetAoEnderecoAnterior() throws Exception {
        ativacao.ativar(convite,SENHA,SENHA);
        assertThat(concorrente(this::mudarEmail,()->credenciais.solicitar(email))).isNull();
        verifyNoInteractions(entrega);
    }
    @Test void desativarImpedeEmissaoConcorrenteDeConvite() throws Exception {
        var usuario=repository.findById(alvo).orElseThrow();
        assertThat(concorrente(this::desativar,()->ativacao.emitir(usuario))).isInstanceOf(OperacaoInvalidaException.class);
    }

    @Test void migracaoRevogaTokensLegadosSemAlterarContasOuTokensConsumidos() {
        // Nome gerado internamente; o schema pertence apenas a este teste descartável.
        String schema="apecan_a01_migracao_"+UUID.randomUUID().toString().replace("-","");
        jdbc.execute("CREATE SCHEMA "+schema);
        try {
            var origem=org.flywaydb.core.Flyway.configure().dataSource(jdbc.getDataSource())
                .schemas(schema).defaultSchema(schema).createSchemas(false).target("11").load();
            origem.migrate();
            Long pendente=jdbc.queryForObject("""
                INSERT INTO %s.usuarios (nome,login,cpf,email,foto_url,telefone,tipo_de_perfil,
                    status,primeiro_acesso_pendente,senha_hash,senha_definitiva_em)
                VALUES ('Convite legado','convite.legado','52998224725','convite@example.invalid',
                    '/images/usuario-padrao.svg','5514999999999','USUARIO','ATIVO',TRUE,NULL,NULL)
                RETURNING id_usuario
                """.formatted(schema),Long.class);
            Long ativo=jdbc.queryForObject("""
                INSERT INTO %s.usuarios (nome,login,cpf,email,foto_url,telefone,tipo_de_perfil,
                    status,primeiro_acesso_pendente,senha_hash,senha_definitiva_em,desativado_em)
                VALUES ('Reset legado','reset.legado','11144477735','reset@example.invalid',
                    '/images/usuario-padrao.svg','5514999999999','USUARIO','INATIVO',FALSE,
                    'hash-ficticio-migracao',TIMESTAMP '2026-01-01 10:00:00',TIMESTAMP '2026-01-02 10:00:00')
                RETURNING id_usuario
                """.formatted(schema),Long.class);
            jdbc.update("""
                INSERT INTO %s.tokens_credencial
                    (id_usuario,token_hash,finalidade,solicitado_em,expira_em,utilizado_em)
                VALUES (?,repeat('a',64),'ATIVACAO',TIMESTAMP '2026-01-01 10:00:00',TIMESTAMP '2099-01-01 10:00:00',NULL),
                    (?,repeat('b',64),'REDEFINICAO_SENHA',TIMESTAMP '2098-01-01 10:00:00',TIMESTAMP '2099-01-01 10:00:00',NULL),
                    (?,repeat('c',64),'REDEFINICAO_SENHA',TIMESTAMP '2026-01-01 10:00:00',TIMESTAMP '2099-01-01 10:00:00',TIMESTAMP '2026-01-01 10:05:00')
                """.formatted(schema),pendente,ativo,ativo);
            var contasAntes=jdbc.queryForList("SELECT * FROM "+schema+".usuarios ORDER BY id_usuario");
            var consumidoAntes=jdbc.queryForMap("SELECT * FROM "+schema+".tokens_credencial WHERE token_hash=repeat('c',64)");

            var corrigido=org.flywaydb.core.Flyway.configure().dataSource(jdbc.getDataSource())
                .schemas(schema).defaultSchema(schema).createSchemas(false).target("12").load();
            assertThat(corrigido.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM "+schema+".tokens_credencial WHERE utilizado_em IS NULL",Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM "+schema+".tokens_credencial WHERE utilizado_em>=solicitado_em",Integer.class)).isEqualTo(3);
            assertThat(jdbc.queryForList("SELECT * FROM "+schema+".usuarios ORDER BY id_usuario")).isEqualTo(contasAntes);
            assertThat(jdbc.queryForMap("SELECT * FROM "+schema+".tokens_credencial WHERE token_hash=repeat('c',64)")).isEqualTo(consumidoAntes);

            // A migração é única: um novo token emitido depois dela continua disponível.
            jdbc.update("""
                INSERT INTO %s.tokens_credencial (id_usuario,token_hash,finalidade,solicitado_em,expira_em)
                VALUES (?,repeat('d',64),'ATIVACAO',TIMESTAMP '2026-01-01 10:00:00',TIMESTAMP '2099-01-01 10:00:00')
                """.formatted(schema),pendente);
            assertThat(corrigido.migrate().migrationsExecuted).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM "+schema+".tokens_credencial WHERE token_hash=repeat('d',64) AND utilizado_em IS NULL",Integer.class)).isEqualTo(1);
        } finally {
            jdbc.execute("DROP SCHEMA "+schema+" CASCADE");
        }
    }
}
