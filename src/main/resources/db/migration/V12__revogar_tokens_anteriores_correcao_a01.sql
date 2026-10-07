-- A versão anterior não vinculava tokens às mudanças de e-mail/status.
-- Não é possível distinguir retrospectivamente quais links ainda são confiáveis.
-- Revoga apenas links pendentes; preserva senhas, contas e tokens já consumidos.
UPDATE tokens_credencial
SET utilizado_em = GREATEST(CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo', solicitado_em)
WHERE utilizado_em IS NULL;
