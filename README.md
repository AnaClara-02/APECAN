# APECAN

Sistema integrado para administracao e gerenciamento da APECAN, desenvolvido
com Spring Boot, Thymeleaf, Spring Security, PostgreSQL e Flyway.

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

Os testes usam banco H2 isolado e não alteram o PostgreSQL local.

## Regras de seguranca do acesso

- contas são criadas sem senha e aguardam ativação;
- o token original é exibido apenas no momento da emissão;
- somente o hash SHA-256 do token é persistido;
- senhas usam `DelegatingPasswordEncoder` com BCrypt;
- não existe cadastro público;
- cada administrador possui conta individual;
- o último administrador ativo não pode ser desativado ou rebaixado;
- alterações de perfil e status ficam registradas no histórico administrativo.
