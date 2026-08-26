# Ajuda para desenvolvimento

Consulte:

- `README.md` para configuracao, execucao e testes;
- `docs/CONTEXTO_DO_PROJETO.md` para arquitetura e decisoes;
- `src/main/resources/db/migration` para o historico PostgreSQL.

Comandos principais no PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```
