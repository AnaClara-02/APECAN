# Hospedagem: Render Free, Neon e Brevo

## Estado e limites

Esta configuração prepara a aplicação; não certifica prontidão para dados reais.
O Render Free suspende por inatividade, pode reiniciar e tem disco efêmero e
512 MB de RAM. Sessões são locais à JVM: reiniciar exige novo login.
Não usar pings artificiais para impedir a suspensão.

O banco será criado vazio no Neon, PostgreSQL 18, AWS São Paulo.
O Render ficará na Virgínia: mesmo com o banco no Brasil, o backend processará
dados no exterior. Avaliar condições contratuais e privacidade antes do uso real.
Não há transferência automática do PostgreSQL local.

A API gratuita do Brevo substitui SMTP neste ambiente. Os provedores podem alterar
quotas; consultar os painéis antes de habilitar usuários reais:

- [Render Free](https://render.com/docs/free)
- [Neon](https://neon.com/pricing)
- [Brevo](https://help.brevo.com/hc/en-us/articles/208580669-FAQs-What-are-the-limits-of-the-Free-plan)

## 1. Preparar banco e permissões

Crie um projeto Neon em São Paulo e o banco `apecan` com PostgreSQL 18.
Escolha o endpoint **direto**, sem `-pooler`, no painel de conexão.
Não execute os comandos no banco local já utilizado.

Como administrador do banco novo, crie os papéis abaixo. Defina as senhas pela
interface administrativa ou pelo comando interativo `\password` do psql;
não coloque senhas em SQL versionado ou no histórico do terminal.

```sql
CREATE ROLE apecan_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
CREATE ROLE apecan_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT CONNECT ON DATABASE apecan TO apecan_migrator, apecan_app;
GRANT USAGE, CREATE ON SCHEMA public TO apecan_migrator;
GRANT USAGE ON SCHEMA public TO apecan_app;
```

Conecte-se como `apecan_migrator` e configure os privilégios para os objetos que
esse papel criará com Flyway:

```sql
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO apecan_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO apecan_app;
```

Após a primeira migração, ainda como migrator:

```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO apecan_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO apecan_app;
GRANT TRUNCATE ON pacientes, historico_status_paciente, voluntarios,
    categorias_equipamentos, equipamentos, emprestimos_equipamentos,
    doacoes, doacao_voluntarios, movimentacoes_financeiras, despesas TO apecan_app;
REVOKE INSERT, UPDATE, DELETE ON flyway_schema_history FROM apecan_app;
```

Não usar o papel administrativo fornecido pelo Neon como usuário cotidiano da
aplicação. A separação do Flyway reduz privilégios da conexão de negócio, mas
as credenciais de migração ainda existem no processo: uma futura implantação
mais restritiva deve executar migrações fora do servidor web.

## 2. Configurar Brevo

Valide o remetente e autentique o domínio quando disponível. Crie uma **chave de
API**, não uma chave SMTP. A aplicação usa HTTPS, porta 443, sem fallback SMTP.

E-mails contêm somente instruções de acesso e links temporários, sem dados de
pacientes. Desabilite rastreamento de cliques/abertura para mensagens de acesso no
provedor, quando disponível, e restrinja acesso aos seus registros transacionais.
Resposta HTTP 201 significa aceitação pelo Brevo, não entrega na caixa postal.

Falha de envio não apaga a conta cadastrada. A tela informa que o envio não foi
confirmado; consulte o usuário e reemita a ativação. A recuperação de senha mantém
uma mensagem genérica, mesmo quando a conta não existe ou o envio falha. Não há
fila durável nem repetição automática de envio: uma interrupção entre commit e
envio exige nova solicitação. O token original não é persistido.

## 3. Segredos e configuração Render

Use `render.yaml` como Blueprint. Ele não cria Neon nem Brevo.
A implantação é manual, após CI aprovada. Não há push automático por esta entrega.

| Variável | Finalidade |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod,render` |
| `APECAN_PUBLIC_URL` | URL HTTPS final, sem query string |
| `APECAN_DB_URL` | JDBC direto do Neon, com TLS verificado |
| `APECAN_DB_USERNAME`, `APECAN_DB_PASSWORD` | Credenciais de `apecan_app` |
| `APECAN_DB_MIGRATION_USERNAME`, `APECAN_DB_MIGRATION_PASSWORD` | Credenciais de `apecan_migrator` |
| `APECAN_BREVO_API_KEY` | Chave da API Brevo |
| `APECAN_MAIL_FROM` | Remetente validado |
| `APECAN_ACCESS_HMAC_KEY` | Segredo aleatório de pelo menos 32 caracteres; gerado pelo Blueprint |

Exemplo **sem credenciais** da URL para o container:

```text
jdbc:postgresql://SEU_ENDPOINT_DIRETO/apecan?sslmode=verify-full&sslrootcert=/etc/ssl/certs/ca-certificates.crt
```

A URL não deve conter usuário nem senha. Não substituir `verify-full` por
`require` nem desabilitar verificação do certificado. Localmente, use o caminho
de um arquivo PEM contendo autoridades certificadoras confiáveis. Não copie
apenas um certificado de servidor temporário nem instale certificados não verificados.

O backup SQL usa a própria conexão JDBC da aplicação e, portanto, a mesma
validação TLS completa. Não há uma credencial separada de backup.

Não trocar a chave HMAC rotineiramente: sua troca impede correlacionar controles
anteriores e reinicia efetivamente os bloqueios. A V10 guarda apenas identificadores
HMAC, contadores e tempos. Registros vencidos são removidos durante novos acessos.
Recuperação: uma solicitação por minuto e no máximo três por hora por identificador.
Sessões permanecem em memória; não habilitar múltiplas instâncias.

## 4. Primeiro administrador

Execute o bootstrap **no computador do responsável**, nunca no serviço Render.
Guarde uma configuração dedicada em `config/application-secrets.properties`,
ignorada pelo Git, com os dados do Neon, CA local, remetente, chave Brevo e HMAC.
Não reutilize inadvertidamente a configuração do PostgreSQL local.

Para esse comando configure:

```properties
spring.datasource.url=JDBC_NEON_COM_VERIFY_FULL_E_CA_LOCAL
spring.datasource.username=apecan_app
spring.datasource.password=PREENCHER_LOCALMENTE
spring.flyway.user=apecan_migrator
spring.flyway.password=PREENCHER_LOCALMENTE
apecan.mail.transporte=brevo
APECAN_PUBLIC_URL=https://SEU_SERVICO.onrender.com
APECAN_BREVO_API_KEY=PREENCHER_LOCALMENTE
APECAN_MAIL_FROM=REMETENTE_VALIDADO
APECAN_ACCESS_HMAC_KEY=PREENCHER_COM_O_MESMO_SEGREDO_DO_RENDER
```

Compile o JAR e execute:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
java -jar target/apecan-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod,bootstrap-admin --spring.config.additional-location=file:./config/application-secrets.properties
```

O Flyway aplica V1–V11. Informe os dados solicitados, sem definir a senha do
administrador. A conta fica pendente e recebe a ativação por e-mail. Inicie o
serviço Render para que o titular abra o link e escolha a senha.

Se o envio inicial falhar, corrija o Brevo e execute o mesmo comando acrescentando
`--reenviar-ativacao-inicial`. Essa opção funciona apenas quando existe uma única
conta, administradora, ativa e ainda não ativada. Após a ativação, reemissões são
feitas pela administração autenticada. Não recrie nem apague usuários pelo SQL.

## 5. Build, execução e verificação

```powershell
docker build -t apecan:render .
docker run --rm --memory=512m --cpus=0.1 --env-file .env.render -p 127.0.0.1:8080:8080 apecan:render
```

O arquivo `.env.render` deve ser privado, ignorado e conter somente configurações
de teste. Para navegação completa com cookie Secure, use acesso HTTPS; o comando
acima serve inicialmente para observar inicialização e memória, não para desabilitar
o cookie em produção.

O limite inicial do heap é 256 MB, com 20 threads HTTP e cinco conexões Hikari.
Isso não garante caber em 512 MB: teste também relatórios PDF/XLSX e backup,
medindo o consumo total com `docker stats`. Não liberar se houver OOM.
Nenhum arquivo persistente pode depender do disco do Render.

O health check é `/actuator/health/liveness`, sem consulta ao Neon/Brevo e sem
detalhes internos. Ele não comprova funcionamento do banco nem a entrega de e-mail.
Valide login e consultas após deploy. Teste suspensão, retomada e necessidade de
novo login. Deploy de versão anterior não desfaz migrações: nunca executar
`flyway clean` para corrigir incompatibilidade.

## 6. Backup e restauração

Administradores usam **Backup > Exportação**, confirmam a senha atual e baixam
um `.sql` com os dados operacionais. O arquivo não contém contas, hashes de senha,
tokens ou permissões; preserva os nomes históricos dos autores. Faça a exportação
ao final de cada dia e antes de manutenção.

O SQL não é criptografado e contém dados pessoais legíveis. Guarde-o em mídia
distinta, protegida e fora do disco efêmero do Render. O backup manual não
substitui uma política formal de recuperação.

Na instalação de destino, crie e ative primeiro um administrador. Em **Backup >
Importação**, selecione o SQL. O sistema valida o formato, compara arquivo e banco
atual e exige novamente a senha antes de substituir as tabelas operacionais.
Contas locais são preservadas. A substituição usa uma transação única; se qualquer
linha falhar, os dados anteriores permanecem.

Depois da importação, valide relacionamentos, IDs, relatórios e login. Registros
mostram o autor e a data originais e, quando recuperados, a data de importação.

O perfil Render limita tanto a exportação SQL quanto seu upload a 5 MB
(requisição multipart de até 6 MB). A aplicação recusa configurações em que
o tamanho permitido para exportação excede o upload. Para bases maiores,
planeje uma janela de restauração e infraestrutura com limites compatíveis;
um arquivo acima do limite não é entregue como backup completo.

## 7. Critérios antes de dados reais

- Homologação integral com dados fictícios, incluindo um restore comprovado.
- MFA nas contas Render, Neon, Brevo e GitHub; acesso operacional restrito.
- Responsáveis definidos para backups, restauração e incidentes.
- Avaliação de privacidade e do processamento internacional.
- Confirmar quotas, remetente, recebimento real dos e-mails e logs sem dados pessoais.
- Testar memória e disponibilidade sob carga representativa.
- Sem sucesso nesses critérios, não considerar o plano gratuito aprovado para uso
  cotidiano. Migrar a mesma imagem para infraestrutura adequada.

As verificações externas não são automáticas: esta entrega não cria contas,
não publica o sistema, não envia dados reais e não substitui essas validações.
