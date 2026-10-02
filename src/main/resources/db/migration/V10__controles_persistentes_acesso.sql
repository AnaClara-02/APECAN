-- Identificadores HMAC; nenhum login, e-mail ou token original.
CREATE TABLE controles_acesso (
    chave VARCHAR(80) PRIMARY KEY,
    falhas INTEGER NOT NULL,
    inicio_janela BIGINT NOT NULL,
    bloqueado_ate BIGINT NOT NULL,
    ultimo_evento BIGINT NOT NULL,
    expira_em BIGINT NOT NULL,
    CONSTRAINT ck_controles_acesso_falhas CHECK (falhas >= 0)
);
CREATE INDEX ix_controles_acesso_expiracao ON controles_acesso(expira_em);
