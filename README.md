# APECAN

Sistema integrado para administracao e gerenciamento da APECAN, desenvolvido
com Spring Boot, Thymeleaf, Spring Security, PostgreSQL e Flyway.

Documentação técnica, decisões deste chat, limitações e próximos passos:
[Contexto do projeto](docs/CONTEXTO_DO_PROJETO.md).

Preparação de hospedagem, configuração de segredos, primeiro administrador e
recuperação técnica: [Render, Neon e Brevo](docs/HOSPEDAGEM_RENDER_NEON_BREVO.md).
O perfil Render usa API HTTPS do Brevo, não SMTP. Dockerfile e Blueprint estão
incluídos; nenhum deploy ou envio do banco local é automático.

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
- backup operacional manual em SQL, sem contas de acesso e com importação transacional.

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

A migração atual é a V11. Ela preserva a autoria histórica dos registros
transportados sem levar contas de acesso; a V10 persiste controles de acesso
com identificadores HMAC.
As exportações não armazenam os arquivos no servidor: eles são gerados sob
demanda, enviados com `Cache-Control: no-store` e registrados apenas por tipo,
formato, filtros não pessoais, quantidade, responsável e data.

## Backup e recuperação

Administradores e usuários comuns usam **Backup > Exportação** para gerar um arquivo `.sql` com
os dados operacionais. Pacientes, voluntários, equipamentos, empréstimos,
doações e financeiro são incluídos, juntamente com os nomes históricos dos
autores. Contas, senhas, tokens, sessões e permissões não são exportados.

O SQL não é criptografado e contém dados pessoais legíveis. Ele deve ser
guardado em mídia protegida e fora do computador servidor.

A exportação SQL usa uma transação de leitura `REPEATABLE_READ`, para que
contagens e tabelas representem o mesmo instante. No Render, exportação e
upload têm limite de 5 MB. Fora desse perfil, o limite padrão continua em 2 GB.
A inicialização recusa configurações em que o backup não cabe no upload ou
falta 1 MB adicional no limite total da requisição multipart.

Em **Backup > Importação**, o APECAN aceita somente a estrutura SQL gerada por
ele próprio. Antes da substituição, exibe nome, data, tamanho e quantidade de
registros do arquivo ao lado das informações dos dados atuais. A confirmação
exige a senha atual do usuário autenticado, preserva as contas da instalação e aplica toda
a substituição em uma única transação.

Administradores e usuários comuns podem exportar e importar backups. As demais
áreas administrativas continuam exclusivas dos administradores. A importação
pode substituir dados operacionais: confira a comparação antes de confirmar.

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
- a ativação local exibe o token apenas ao administrador que o emite;
- tokens de redefinição nunca aparecem na resposta pública; a recuperação de
  senha requer entrega por e-mail (`APECAN_PASSWORD_RESET_DELIVERY=email`);
- somente o hash SHA-256 do token é persistido;
- senhas usam `DelegatingPasswordEncoder` com BCrypt;
- não existe cadastro público;
- cada administrador possui conta individual;
- um administrador não pode desativar a própria conta, mesmo havendo outro administrador ativo;
- o último administrador ativo não pode ser desativado ou rebaixado;
- alterações de perfil e status ficam registradas no histórico administrativo.
- restauração completa exige o perfil `recovery`, listener de loopback e
  `server.forward-headers-strategy=none`; banco vazio não habilita esse acesso;
- troca e redefinição de senha revogam os tokens de ativação e recuperação anteriores;
- mudança efetiva de e-mail, desativação e reativação também revogam todos os
  links pendentes, na mesma transação da alteração da conta;
- confirmação de senha tem limite persistente de cinco falhas, com bloqueio
  de 15 minutos compartilhado entre troca de senha, administração e backup.

## Limites dos relatórios

PDF e XLSX são gerados em arquivo temporário, lidos em lotes e apagados ao
final do download, inclusive em falhas. Uma exportação por instância pode
ficar em andamento, incluindo sua transmissão. Os padrões são 10.000 registros,
20 MB de saída e 60 segundos de geração; arquivos acima do limite são recusados,
sem truncar dados. Configure `apecan.relatorios.max-registros`,
`apecan.relatorios.max-bytes` e `apecan.relatorios.timeout-segundos` dentro dos
tetos de 50.000 registros, 50 MB e 120 segundos. Revise capacidade antes de elevar
os padrões.

## Cuidados antes da produção

Dados reais exigem infraestrutura de produção com HTTPS, banco protegido,
backups e restauração testada, segredos gerenciados pela hospedagem e controle
de acesso operacional. Homologar a implantação gratuita somente com dados
fictícios ou anonimizados. O uso cotidiano com dados reais depende dos critérios
de liberação do guia de hospedagem; o Render desaconselha Free para produção.
Antes de executar mais de uma instância, persistir sessões HTTP em armazenamento
compartilhado e revisar a invalidação das sessões. Os controles de tentativas de
login e recuperação já são persistidos no PostgreSQL pela V10; a V11 implementa
o transporte SQL dos dados operacionais sem transportar contas.

A V12 invalida uma única vez os links pendentes emitidos antes da correção A01.
Após a atualização, reenvie convites de ativação necessários; quem precisar
recuperar a senha deve solicitar um novo link. Senhas e cadastros são preservados.

## Perfis administrativos e exclusões de teste

As migrações V13 e V14 adicionam o perfil **Adm. Dev.** e a auditoria de
exclusões operacionais. Ao aplicar V13, contas que eram `ADMINISTRADOR` passam
a `ADM_DEV`, preservando ID, senha, status e vínculos. A conversão é anotada no
histórico com responsável técnico nulo; nenhum novo usuário é criado. O perfil
**Administrador** continua disponível para promoção posterior.

| Perfil | Acesso |
|---|---|
| Adm. Dev. | Administração atual, gestão dos três perfis e exclusões operacionais de teste |
| Administrador | Administração atual de contas Administrador/Usuário; não gerencia Adm. Dev. |
| Usuário | Operações atuais e exportação/importação de backup |

Todos os perfis administrativos precisam usar contas individuais. A tela de
usuários permite alterar o perfil apenas com senha atual, justificativa e conta
ativa e ativada. Não se pode desativar a própria conta, remover o último
administrador ativo nem o último Adm. Dev. ativo e ativado. A configuração
inicial local cria um Adm. Dev. pendente, sem senha padrão; a hospedagem Render
continua proibindo o modo de bootstrap.

Somente Adm. Dev. vê a ação **Excluir teste** para registros operacionais. A
confirmação exige senha, justificativa e a palavra `EXCLUIR`, apresenta os
vínculos e o impacto, e grava tipo, ID, responsável, horário e quantidade na
tabela `auditoria_exclusoes_teste`. A exclusão é definitiva e não deve ser usada
em dados reais de atendimento. Faça e confira um backup antes dos testes.

Pacientes com empréstimos, voluntários associados a doações, equipamentos com
histórico de empréstimos, categorias com unidades e doações de equipamentos com
unidades ficam bloqueados até que suas dependências sejam resolvidas. Excluir
empréstimo aberto libera o equipamento como ATIVO sem inventar devolução;
doação monetária e despesa removem sua movimentação correspondente na mesma
transação. Movimentação ligada a uma origem deve ser removida pela doação ou
despesa. Categorias vazias mantêm a ação já existente para os demais perfis.

O backup SQL operacional continua transportando somente os dados operacionais
permitidos. Ele não leva contas, hashes de senha, tokens, permissões nem as
auditorias administrativas, de backup, exportação ou exclusão. V13 e V14 são
migrações novas: faça backup do banco antes de publicar a versão que as aplica,
homologue a conversão de perfis e confirme a presença das duas migrações no
ambiente de destino. A implementação local não aplicou migrações nem alterou o
banco hospedado.
