# Contexto do Projeto APECAN

Documentação técnica e memória de decisões do chat, revisadas em 26/09/2026.
Este documento descreve o código disponível; planos e relatos de uso não são
equivalentes a testes de aceitação concluídos. Não contém transcrição integral,
credenciais, tokens nem dados pessoais do atendimento.

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

O package raiz e `com.aclg.apecan`. Os packages `auth`, `usuario`, `paciente`, `voluntario`, `equipamento`, `emprestimo`, `doacao`, `financeiro`, `despesa`, `relatorio` e `backup` representam funcionalidades. `shared` contem recursos transversais de auditoria, excecoes e validacao.

```text
controller -> service -> repository -> PostgreSQL
     DTOs de entrada/saida; entidades JPA na persistencia
     mapper converte entidades e DTOs quando necessario
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
- entrega local, SMTP e API HTTPS Brevo, selecionáveis por configuração;
- limitação progressiva persistente de login e limites de recuperação de senha;
- pacientes, historico de status e inativacao sem exclusao;
- voluntarios, categorias e unidades individuais de equipamentos;
- emprestimos com bloqueio pessimista, versao otimista e devolucao auditada;
- filtros de pacientes, voluntarios, equipamentos e emprestimos iniciados em
  `ATIVO`, mantendo as opções `INATIVO` e `TODOS` na navegação paginada;
- exclusao de categorias somente quando não existe unidade vinculada, com
  bloqueio transacional contra cadastro concorrente;
- historico paginado de emprestimos em cada equipamento;
- doacao monetaria e entrada financeira atomicas;
- doacao de equipamento e criacao de unidades no estoque atomicas;
- despesa e movimentacao de saida atomicas;
- filtros, paginacao e painel de relatorios;
- exportacoes detalhadas PDF/XLSX com CPF mascarado, proteção contra formulas
  em planilhas e auditoria;
- modo noturno acessível, com preferência persistida no navegador.
- backup operacional em arquivo `.sql`, sem contas de acesso, preservando nomes
  de autoria e com importação transacional confirmada pelo usuário autenticado.

## Banco e configuracao

O Flyway e o unico responsavel por evoluir o esquema. Nenhuma migracao aplicada deve ser alterada. O conjunto atual e:

- V1: esquema funcional inicial, restricoes, indices e view de equipamentos;
- V2: alinhamento de tipos textuais entre entidades e PostgreSQL;
- V3: ativacao e historico administrativo de usuarios;
- V4: eventos de redefinicao e alteracao de senha;
- V5: autoria e concorrencia de equipamentos e devolucoes;
- V6: previsao de devolucao e indice de emprestimos atrasados.
- V7: normalizacao dos telefones brasileiros com código do país `55`;
- V8: historico seguro das exportacoes de relatorios.
- V9: auditoria de exportacoes de backup e restauracoes locais.
- V10: controles persistentes de acesso com identificadores HMAC, sem login/e-mail em claro.
- V11: autoria histórica e exportação/importação SQL de dados operacionais sem contas.

Credenciais locais ficam em `config/application-local.properties`, ignorado pelo Git. O arquivo contem apenas URL, usuario e senha do PostgreSQL tecnico. Senhas, tokens, chaves e dados pessoais reais nunca devem ser adicionados a arquivos versionados.

O perfil `prod` exige URL publica HTTPS, cookie seguro, banco configurado e entrega de ativacao/redefinicao por e-mail. Segredos de producao devem ser fornecidos pela plataforma de hospedagem.

## Execucao e testes

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```

Acesse `http://localhost:8080/login`. Testes rapidos usam H2; a verificacao PostgreSQL/Flyway usa Testcontainers quando Docker esta disponivel.

O menu **Backup** possui Exportação e Importação. A exportação gera SQL legível
com dados operacionais, sem usuários e credenciais. A importação compara arquivo
e banco atual, exige nova confirmação da senha e substitui apenas os dados
operacionais em uma transação. As contas da instalação permanecem intactas.

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
- registros de negocio sao inativados; a excecao aprovada e excluir categorias sem unidades vinculadas;
- CPFs sao persistidos com 11 numeros;
- dados reais somente podem ser usados em infraestrutura de producao aprovada;
- homologação e demonstrações recebem apenas dados fictícios ou anonimizados;
- o uso real pretendido em hospedagem gratuita não está aprovado só pela implementação;
- segredos nunca sao versionados.

## Proximos passos

- ampliar testes de concorrencia real para exclusao/cadastro de categorias;
- instalar Docker localmente para executar a validacao PostgreSQL/Testcontainers;
- realizar teste de usabilidade com dados totalmente ficticios;
- revisar com a APECAN a obrigatoriedade da nota fiscal e os relatorios;
- homologar Brevo, HTTPS e infraestrutura protegida, e testar periodicamente a
  restauracao dos backups antes
  de inserir dados reais;
- persistir sessões HTTP e revisar sua invalidação antes de escalar o backend para
  mais de uma instância; tentativas de login já são persistentes;
- criar armazenamento protegido para fotos antes de habilitar uploads;
- preparar implantacao e observabilidade sem registrar dados pessoais.

## Estrutura dos arquivos

| Caminho | Finalidade |
| --- | --- |
| `pom.xml` | Dependências e compilação com alvo Java 25; versão da aplicação `0.0.1-SNAPSHOT`. |
| `mvnw`, `mvnw.cmd`, `.mvn/` | Maven Wrapper, para execução sem instalar Maven separadamente. |
| `src/main/java/com/aclg/apecan/ApecanApplication.java` | Ponto de entrada e descoberta dos componentes Spring. |
| `src/main/java/com/aclg/apecan/` | Módulos funcionais e infraestrutura compartilhada. |
| `src/main/resources/templates/` | Páginas Thymeleaf, formulários e fragments reutilizados. |
| `src/main/resources/static/` | CSS, JavaScript, imagens e fontes da interface. |
| `src/main/resources/db/migration/` | Migrações imutáveis V1–V11. |
| `src/main/resources/application*.properties` | Configurações compartilhadas e perfis, sem credenciais reais. |
| `src/main/resources/META-INF/` | Metadados das propriedades próprias, usados pelo editor; não são segredos. |
| `config/application-local.properties` | Configuração privada externa ao JAR, ignorada no Git. |
| `scripts/apecan-recovery.ps1` | Inicialização local do modo de recuperação e controle do serviço quando configurado. |
| `src/test/` | Testes automatizados e configurações isoladas de teste. |
| `.github/workflows/` | CI com Java 25 e Maven verify. |
| `target/` | Classes, JAR e relatórios gerados; não versionar. |
| `docs/` | Documentação persistente do projeto. |

## Contexto e decisões consolidadas do chat

1. O sistema foi planejado para o trabalho de graduação e atendimento da APECAN.
   A organização evoluiu de propostas por camadas para package by feature.
   Não há frontend SPA separado: HTML é renderizado pelo Thymeleaf.
2. PostgreSQL local é a base adotada. pgAdmin é ferramenta administrativa, não
   componente usado pelo navegador para acessar dados. Supabase foi descartado
   nesta etapa. O usuário técnico do banco é distinto das contas de `usuarios`.
3. Cada conta possui um único perfil (`ADMINISTRADOR` ou `USUARIO`). Foram
   descartadas senhas temporárias: o destinatário define sua senha por ativação.
   A administração deve ser transferível, preservando ao menos um administrador.
4. CPF é armazenado com 11 dígitos; telefone brasileiro recebe máscara variável
   de oito ou nove dígitos locais e armazenamento numérico com código `55`.
   Máscaras não substituem validação no servidor e restrições do banco.
5. Equipamentos são unidades individuais de uma categoria, identificadas por ID,
   sem patrimônio, número de série, marca ou modelo. Estados operacionais:
   `ATIVO`, `INATIVO`, `EMPRESTADO`; totais são consultados por categoria.
6. Doações monetárias e despesas geram suas movimentações financeiras na mesma
   transação. Valor e data financeiros não devem ser duplicados na despesa.
   A nota fiscal é um número na despesa, não uma tabela ou upload de documento.
   Doações de equipamentos geram unidades no estoque; outros bens têm seu fluxo.
7. Foram incorporados filtros inicialmente ativos, histórico de empréstimos por
   equipamento, tema noturno e exportações PDF/XLSX. Formulários de doação mostram
   os campos pertinentes ao tipo, com máscara monetária na interface.
8. A implantação local pode centralizar aplicação e banco em um servidor;
   os demais computadores usam o navegador pela rede. O banco não precisa ficar
   exposto aos computadores clientes. Segurança do backend não substitui proteção
   do sistema operacional, rede, disco e cópias de segurança.
9. O usuário informou que haverá dados reais. A recomendação registrada é não
   usar ambientes gratuitos de demonstração para esses dados. A implantação
   exige avaliação operacional, acesso restrito, HTTPS e restauração testada.
10. Backup operacional foi definido como SQL controlado. Ele não transporta
    contas, senhas ou permissões; preserva o nome e a data do autor histórico.
    Relatórios continuam sendo exportações de consulta, não backups.
11. A importação é feita por um usuário autenticado (administrador ou usuário comum)
    já criado na instalação de destino.
    Ela compara arquivo e dados atuais, pede confirmação e substitui somente as
    tabelas operacionais, preservando as contas locais.
12. O documento acadêmico foi revisado usando a versão mais recente fornecida em
    ZIP como base, conforme escolha explícita do usuário. Foram alinhados
    tecnologias, RF01–RF16, diagrama e alegações de resultados. PDF e LaTeX
    revisados foram entregues separadamente e não fazem parte deste repositório.

### Atualização de interface e permissões — 08/10/2026

- As telas e páginas de erro usam `system-ui` e fontes locais de fallback, sem
  baixar Nunito para a interface. A fonte dos relatórios PDF foi preservada.
- O menu Backup e as rotas de exportação/importação permitem `ADMINISTRADOR` e
  `USUARIO`; confirmação de senha, CSRF, autoria e validação do arquivo permanecem.
- As demais áreas administrativas continuam exclusivas do administrador.
- A desativação da própria conta administrativa é proibida no service e omitida
  na tela; a desativação de outras contas mantém a proteção do último administrador.

## Operação segura e diagnóstico

- Executar os comandos do README na raiz do projeto; parar com `Ctrl+C`.
- Para gerar o JAR, usar `.\mvnw.cmd --batch-mode --no-transfer-progress package`.
  O artefato é `target/apecan-0.0.1-SNAPSHOT.jar`; usar o perfil local e manter a
  configuração externa ao executá-lo com `java -jar` a partir da raiz.
- A entrega `local` de links não envia e-mail. SMTP exige o adaptador `email`,
  servidor e credenciais válidos, guardados fora dos arquivos públicos.
- O arquivo `.sql` é baixado para o local escolhido pelo navegador, normalmente
  Downloads. Copiar para outro dispositivo protegido; não versionar, pois contém
  dados pessoais legíveis e não possui criptografia própria.
- A importação substitui dados operacionais; não mescla cadastros. A interface
  mostra a comparação antes da confirmação e mantém os usuários da instalação.
- Não publicar `application-local.*`, `application-secrets.*`, `.env`, backups,
  dumps, logs, certificados, chaves privadas ou arquivos com dados reais.

## Limitações e validações pendentes do backup

O código implementa criptografia, checksums, auditoria e restauração transacional
com ferramentas PostgreSQL. Isso não significa certificação de recuperação em
todas as falhas. Os testes atuais de backup abrangem criptografia, acesso ao
controller e disponibilidade local. O teste PostgreSQL agora também implementa
dump, criptografia e restore em outro banco descartável, dependendo de Docker.

Antes do uso operacional, priorizar:

- teste integral de exportar/restaurar em PostgreSQL separado, incluindo IDs,
  relacionamentos, autenticação, tokens invalidados e migrações;
- testes de interrupção, falta de espaço e falha de reversão;
- a cópia técnica passou a ser preservada se a reversão falhar; continua sem ser
  uma cópia externa durável contra pane, e requer proteção e limpeza operacional;
- revisão dos limites de upload, permissões dos temporários e exclusão mútua;
- instalador e serviço Windows: existe script de recuperação, não um instalador
  `APECAN-Recovery.exe` entregue;
- homologar o transporte remoto: o cliente agora repassa opções TLS e o perfil
  Render exige verificação de certificado/hostname com CA explícita.

## Verificação desta entrega

Em 12/09/2026, `mvnw.cmd --batch-mode --no-transfer-progress test` terminou com
`BUILD SUCCESS`: 51 testes contabilizados, 50 executados com sucesso, nenhuma
falha ou erro e 1 ignorado (integração PostgreSQL/Testcontainers sem Docker
disponível). A execução local usou JDK 26, com alvo Java 25 definido no POM;
a validação específica no JDK 25 permanece a cargo da CI configurada.
Não foi realizada restauração nem alteração do banco operacional nesta entrega.

## Como continuar em outro chat

### Incremento Render, Neon e Brevo — 26/09/2026

- Docker multi-stage Java 25, execução não root e clientes PostgreSQL 18.
- Blueprint Render Free na Virgínia, deploy manual e health check de liveness.
- Neon escolhido em São Paulo e inicialmente vazio; nenhuma transferência do banco local.
- Perfil `prod,render` com pool pequeno, Brevo HTTPS e recuperação pública bloqueada.
- Segredos de banco, Flyway, backup e API separados da imagem e do repositório.
- Interface compartilhada de e-mail; envio após commit e mensagens coerentes com falha/aceitação.
- V10 persiste bloqueios e limites de recuperação com identificadores HMAC.
- Bootstrap local suporta reemissão inicial somente para a única conta administradora pendente.
- Backup SQL usa formato controlado, valores parametrizados na importação e transação única.
- Sessões continuam em memória; reinícios exigem login. Não há fila durável de e-mails.
- O uso real é pretendido, mas depende de homologação, privacidade, recuperação e disponibilidade;
  não foi certificado como pronto para produção.

Procedimento completo e pré-requisitos: [Hospedagem Render/Neon/Brevo](HOSPEDAGEM_RENDER_NEON_BREVO.md).

Verificação desta etapa: `verify` gerou o JAR e contabilizou 67 testes,
66 aprovados e 1 ignorado por ausência de Docker, sem falhas ou erros.
Uma instância PostgreSQL 18.3 independente e descartável também validou V1–V11,
Hibernate `validate` e dump/restore para outro banco. Ela foi encerrada após o teste.
JDK local 26 com alvo 25; execução em Java 25 e build da imagem ficam para a CI.
Permanecem pendentes o ensaio Docker de 512 MB, conexão Neon TLS real, entrega Brevo
e homologação completa dos fluxos operacionais na hospedagem. Não foram usados
dados reais, acessadas contas externas ou realizados commit/push/deploy.

### Orientações de continuidade

Ler este arquivo e o README; conferir `git status`, histórico e código antes de
alterar. Não tomar planos antigos como funcionalidades comprovadas. Preservar
migrações aplicadas e mudanças locais, não ler/expor credenciais sem necessidade,
não usar dados reais em testes e registrar resultados efetivamente executados.

### Perfis e exclusões operacionais — 10/10/2026

- A V13 amplia os perfis para `ADM_DEV`, `ADMINISTRADOR` e `USUARIO` e converte
  contas `ADMINISTRADOR` existentes em `ADM_DEV`, preservando suas credenciais e
  demais dados. O evento da conversão é histórico e não atribuído a uma pessoa.
- A V14 cria `auditoria_exclusoes_teste`; cada evento registra responsável,
  registro-alvo (tipo e ID), justificativa, quantidade removida e data/hora.
- Adm. Dev. mantém as permissões administrativas, gerencia todos os perfis e
  pode excluir certos registros operacionais de teste. Administrador não pode
  gerenciar contas Adm. Dev. Usuário mantém operações e acesso a backups.
- O último Adm. Dev. ativo/ativado e o último administrador ativo são
  protegidos. Alterar perfil/status exige senha atual e justificativa; contas
  alteradas perdem as sessões existentes.
- A exclusão operacional tem confirmação por tela e POST com CSRF; não remove
  contas, tokens ou auditorias. Bloqueia exclusões com dependências, e transações
  unem registros inseparáveis (como doação monetária/movimentação e
  despesa/movimentação). É irreversível e não deve ser usada com dados reais.
- O backup SQL segue sua lista operacional explícita e não exporta usuários nem
  a nova auditoria. Backups anteriores de dados operacionais permanecem
  versionados por seu cabeçalho e podem ser importados sem substituir contas.
- A implementação não executou migrações nem mudou a base Neon/Render, não criou
  contas e não fez commit, push ou deploy. Antes de publicar, gerar backup,
  revisar a conversão das contas existentes e homologar em dados fictícios.
