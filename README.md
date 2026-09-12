# APECAN

Sistema integrado para administracao e gerenciamento da APECAN, desenvolvido
com Spring Boot, Thymeleaf, Spring Security, PostgreSQL e Flyway.

Documentação técnica, decisões deste chat, limitações e próximos passos:
[Contexto do projeto](docs/CONTEXTO_DO_PROJETO.md).

## Funcionalidades

- usuarios, ativacao, login, recuperacao e alteracao de senha;
- administracao transferivel de acessos;
- pacientes e historico de status;
- voluntarios;
- categorias, estoque individual de equipamentos, emprestimos e devolucoes;
- doacoes monetarias, de equipamentos e de outros bens;
- movimentacoes financeiras, despesas e relatorios;
- filtros de status iniciados em itens ativos e historico de cada equipamento;
- exportacao detalhada de relatorios em PDF e XLSX, com auditoria;
- tema claro ou noturno persistido no navegador.
- backup completo manual, criptografado e recuperável localmente.

## Configuracao local

As credenciais locais ficam em `config/application-local.properties`, arquivo
ignorado pelo Git. Ele deve conter somente:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/NOME_DO_BANCO
spring.datasource.username=USUARIO_TECNICO
spring.datasource.password=SENHA_DO_USUARIO_TECNICO
```

## Criar o primeiro administrador

O procedimento funciona apenas enquanto a tabela `usuarios` estiver vazia. No
PowerShell, execute:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local,bootstrap-admin"
.\mvnw.cmd spring-boot:run
```

O terminal solicitará nome, login, CPF, e-mail e telefone. Nenhuma senha será
solicitada. Ao final, ele mostrará uma única vez o link de ativação e encerrará
a aplicação.

Inicie então o servidor normalmente:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

Abra o link mostrado anteriormente, defina a senha definitiva e depois acesse
`http://localhost:8080/login`.

## Execucao normal

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

Para encerrar, pressione `Ctrl+C` no mesmo terminal.

## Testes

```powershell
.\mvnw.cmd test
```

Os testes rápidos usam banco H2 isolado e não alteram o PostgreSQL local. Com
Docker disponível, o Testcontainers também cria um PostgreSQL descartável,
executa todas as migrações Flyway e valida recursos específicos do PostgreSQL.

## Banco de dados

O esquema é criado e evoluído somente pelas migrações de
`src/main/resources/db/migration`. Migrações aplicadas nunca devem ser
editadas; uma mudança exige um novo arquivo `Vn__descricao.sql`.

A migração atual é a V9, que cria a auditoria de backups e restaurações.
As exportações não armazenam os arquivos no servidor: eles são gerados sob
demanda, enviados com `Cache-Control: no-store` e registrados apenas por tipo,
formato, filtros não pessoais, quantidade, responsável e data.

## Backup e recuperação

Administradores acessam `Administração > Backup`, confirmam a própria senha e
definem uma senha exclusiva de pelo menos 14 caracteres para baixar um arquivo
`.apecan-backup`. O arquivo usa `pg_dump` em formato customizado, manifesto,
checksums e criptografia AES-256-GCM. A senha nunca é armazenada; sem ela, não
há como abrir o backup.

Configure os executáveis do PostgreSQL quando eles não estiverem no `PATH`:

```properties
apecan.backup.pg-dump-path=C:/Program Files/PostgreSQL/18/bin/pg_dump.exe
apecan.backup.pg-restore-path=C:/Program Files/PostgreSQL/18/bin/pg_restore.exe
apecan.backup.diretorio-temporario=C:/APECAN/temp
```

Em uma instalação vazia, o botão de restauração aparece no login somente ao
abrir a aplicação no próprio servidor. Em uma instalação que já possui
usuários, pare o serviço e execute, em PowerShell administrativo:

```powershell
.\scripts\apecan-recovery.ps1
```

Durante o desenvolvimento, informe explicitamente a configuração local:

```powershell
.\scripts\apecan-recovery.ps1 `
  -JarPath .\target\apecan-0.0.1-SNAPSHOT.jar `
  -ConfigFile .\config\application-local.properties `
  -ServiceName ""
```

O script inicia temporariamente a recuperação em
`http://127.0.0.1:8090/recuperacao`, inacessível pela rede. Depois da
restauração, todos precisam entrar novamente e tokens antigos de ativação ou
redefinição são invalidados. Nunca mantenha a única cópia no mesmo disco do
servidor.

O Java de compilação e implantação é o Java 25 LTS.

## Regras de seguranca do acesso

- contas são criadas sem senha e aguardam ativação;
- o token original é exibido apenas no momento da emissão;
- somente o hash SHA-256 do token é persistido;
- senhas usam `DelegatingPasswordEncoder` com BCrypt;
- não existe cadastro público;
- cada administrador possui conta individual;
- o último administrador ativo não pode ser desativado ou rebaixado;
- alterações de perfil e status ficam registradas no histórico administrativo.
- restauração completa só é aceita localmente em instalação vazia ou no perfil
  restrito de recuperação.

## Cuidados antes da produção

Dados reais exigem infraestrutura de produção com HTTPS, banco privado,
backups e restauração testada, segredos gerenciados pela hospedagem e controle
de acesso operacional. Planos gratuitos devem ser usados somente com dados
fictícios ou anonimizados. Antes de executar mais de uma instância do backend,
as sessões HTTP e o controle de tentativas de login precisam ser persistidos em
armazenamento compartilhado.
