ALTER TABLE despesas
    ADD COLUMN nome_emitente VARCHAR(150),
    ADD COLUMN cnpj_emitente VARCHAR(14),
    ADD COLUMN data_emissao DATE,
    ADD COLUMN banco_pagamento VARCHAR(10),
    ADD COLUMN outro_banco_pagamento VARCHAR(150);

ALTER TABLE despesas
    ALTER COLUMN numero_nota_fiscal DROP NOT NULL;

ALTER TABLE despesas
    ADD CONSTRAINT ck_despesas_dados_nota_coerentes CHECK (
        (
            nome_emitente IS NULL
            AND cnpj_emitente IS NULL
            AND data_emissao IS NULL
        )
        OR
        (
            nome_emitente IS NOT NULL
            AND BTRIM(nome_emitente) <> ''
            AND cnpj_emitente IS NOT NULL
            AND cnpj_emitente ~ '^[A-Z0-9]{12}[0-9]{2}$'
            AND numero_nota_fiscal IS NOT NULL
            AND BTRIM(numero_nota_fiscal) <> ''
            AND data_emissao IS NOT NULL
        )
    ),
    ADD CONSTRAINT ck_despesas_banco_pagamento CHECK (
        (banco_pagamento IS NULL AND outro_banco_pagamento IS NULL)
        OR (banco_pagamento IS NOT NULL AND banco_pagamento IN ('SICOOB', 'PAGBANK')
            AND outro_banco_pagamento IS NULL)
        OR (banco_pagamento IS NOT NULL AND banco_pagamento = 'OUTRO'
            AND outro_banco_pagamento IS NOT NULL
            AND BTRIM(outro_banco_pagamento) <> '')
    );

ALTER TABLE historico_exportacoes
    DROP CONSTRAINT ck_historico_exportacoes_tipo;

ALTER TABLE historico_exportacoes
    ADD CONSTRAINT ck_historico_exportacoes_tipo CHECK (
        tipo_relatorio IN (
            'PACIENTES',
            'VOLUNTARIOS',
            'EMPRESTIMOS',
            'DOACOES',
            'MOVIMENTACOES_FINANCEIRAS',
            'DESPESAS'
        )
    );
