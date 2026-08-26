# Contexto do Projeto APECAN

## Visao geral e estado atual

O APECAN e um monolito modular Spring Boot organizado por funcionalidade. Backend, interface Thymeleaf, seguranca e persistencia PostgreSQL pertencem a mesma aplicacao. A interface e servida pelo proprio backend, portanto nao existe frontend separado nem comunicacao direta do navegador com o PostgreSQL.

```text
Navegador -> Spring MVC/Thymeleaf -> Services -> Spring Data JPA -> PostgreSQL
```

## Tecnologias

- Java 25 LTS e Spring Boot 4.1.1;
- Thymeleaf, Spring MVC e Spring Security;
- Spring Data JPA, PostgreSQL, Flyway e HikariCP;
- Maven, JUnit, H2 e Testcontainers.

## Arquitetura e packages

O package raiz e `com.aclg.apecan`. Os packages `auth`, `usuario`, `paciente`, `voluntario`, `equipamento`, `emprestimo`, `doacao`, `financeiro`, `despesa` e `relatorio` representam funcionalidades. `shared` contem recursos transversais de auditoria, excecoes e validacao.

```text
controller -> dto -> service -> repository -> entity
                    mapper
```

- `controller`: recebe requisicoes, valida formularios e seleciona a pagina;
- `dto`: contratos de entrada e saida, sem expor entidades JPA;
- `service`: regras de negocio e limites transacionais;
- `repository`: consultas e persistencia Spring Data;
- `entity`: mapeamento do dominio para o banco;
- `mapper`: conversoes mais extensas entre entidade e DTO.

## Funcionalidades implementadas

- criacao terminal do primeiro administrador e ativacao por token de uso unico;
- login por sessao, administracao de usuarios e protecao do ultimo administrador;
- recuperacao e alteracao de senha, com encerramento de sessoes;
- entrega local em desenvolvimento e adaptador SMTP em producao;
- limitacao progressiva em memoria de tentativas de login;
- pacientes, historico de status e inativacao sem exclusao;
- voluntarios, categorias e unidades individuais de equipamentos;
- emprestimos com bloqueio pessimista, versao otimista e devolucao auditada;
- doacao monetaria e entrada financeira atomicas;
- doacao de equipamento e criacao de unidades no estoque atomicas;
- despesa e movimentacao de saida atomicas;
- filtros, paginacao e painel de relatorios.

## Banco e configuracao

O Flyway e o unico responsavel por evoluir o esquema. Nenhuma migracao aplicada deve ser alterada. O conjunto atual e:

- V1: esquema funcional inicial, restricoes, indices e view de equipamentos;
- V2: base compartilhada e ajustes anteriores de auditoria;
- V3: ativacao e historico administrativo de usuarios;
- V4: eventos de redefinicao e alteracao de senha;
- V5: autoria e concorrencia de equipamentos e devolucoes;
- V6: previsao de devolucao e indice de emprestimos atrasados.

Credenciais locais ficam em `config/application-local.properties`, ignorado pelo Git. O arquivo contem apenas URL, usuario e senha do PostgreSQL tecnico. Senhas, tokens, chaves e dados pessoais reais nunca devem ser adicionados a arquivos versionados.

O perfil `prod` exige URL publica HTTPS, cookie seguro, banco configurado e entrega de ativacao/redefinicao por e-mail. Segredos de producao devem ser fornecidos pela plataforma de hospedagem.

## Execucao e testes

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

Acesse `http://localhost:8080/login`. Testes rapidos usam H2; a verificacao PostgreSQL/Flyway usa Testcontainers quando Docker esta disponivel.

Para criar o primeiro administrador em banco vazio:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local,bootstrap-admin"
.\mvnw.cmd spring-boot:run
```

## Testes e integracao continua

A suite cobre autenticacao, autorizacao, CSRF, validacao de CPF, auditoria, pacientes, credenciais e fluxos transacionais operacionais. O teste Testcontainers aplica as migracoes do zero e valida a view e o indice parcial do PostgreSQL. Sem Docker ele e marcado como ignorado; no GitHub Actions ele e executado em Linux com Java 25.

## Decisoes permanentes

- frontend Thymeleaf na mesma aplicacao;
- autenticacao por sessao, sem JWT;
- apenas administradores gerenciam acessos;
- ambos os perfis operam modulos funcionais;
- registros de negocio sao inativados, nunca excluidos pela interface;
- CPFs sao persistidos com 11 numeros;
- dados reais nao sao usados em desenvolvimento ou demonstracao;
- segredos nunca sao versionados.

## Proximos passos

- ampliar testes MVC de cada formulario e cenarios de concorrencia real;
- instalar Docker localmente para executar a validacao PostgreSQL/Testcontainers;
- realizar teste de usabilidade com dados totalmente ficticios;
- revisar com a APECAN a obrigatoriedade da nota fiscal e os relatorios;
- configurar SMTP, HTTPS, backups e restauracao antes de qualquer piloto;
- criar armazenamento protegido para fotos antes de habilitar uploads;
- preparar implantacao e observabilidade sem registrar dados pessoais.
