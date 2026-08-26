ALTER TABLE emprestimos_equipamentos
    ADD COLUMN data_prevista_devolucao DATE,
    ADD CONSTRAINT ck_emprestimos_previsao CHECK (
        data_prevista_devolucao IS NULL OR data_prevista_devolucao >= data_emprestimo
    );

CREATE INDEX ix_emprestimos_previsao_abertos
    ON emprestimos_equipamentos (data_prevista_devolucao)
    WHERE data_devolucao IS NULL;
