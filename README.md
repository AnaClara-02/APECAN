# APECAN

Sistema integrado para administracao e gerenciamento da APECAN, desenvolvido
com Spring Boot, Thymeleaf, Spring Security, PostgreSQL e Flyway.

## Funcionalidades

- usuarios, ativacao, login, recuperacao e alteracao de senha;
- administracao transferivel de acessos;
- pacientes e historico de status;
- voluntarios;
- categorias, estoque individual de equipamentos, emprestimos e devolucoes;
- doacoes monetarias, de equipamentos e de outros bens;
- movimentacoes financeiras, despesas e relatorios.

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
