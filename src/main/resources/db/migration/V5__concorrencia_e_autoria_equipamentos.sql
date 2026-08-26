CREATE UNIQUE INDEX uq_categorias_equipamentos_nome_normalizado
    ON categorias_equipamentos (LOWER(nome));

ALTER TABLE equipamentos
    ADD COLUMN atualizado_por_usuario_id BIGINT,
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_equipamentos_atualizado_por FOREIGN KEY (atualizado_por_usuario_id)
        REFERENCES usuarios (id_usuario);

CREATE INDEX ix_equipamentos_atualizado_por
    ON equipamentos (atualizado_por_usuario_id);

ALTER TABLE emprestimos_equipamentos
    ADD COLUMN devolvido_por_usuario_id BIGINT,
    ADD COLUMN estado_conservacao_devolucao VARCHAR(15),
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_emprestimos_devolvido_por FOREIGN KEY (devolvido_por_usuario_id)
        REFERENCES usuarios (id_usuario),
    ADD CONSTRAINT ck_emprestimos_devolucao_detalhes CHECK (
        (data_devolucao IS NULL AND devolvido_por_usuario_id IS NULL
            AND estado_conservacao_devolucao IS NULL)
        OR
        (data_devolucao IS NOT NULL AND devolvido_por_usuario_id IS NOT NULL
            AND estado_conservacao_devolucao IN ('NOVO', 'BOM', 'REGULAR', 'DANIFICADO'))
    );

CREATE INDEX ix_emprestimos_devolvido_por
    ON emprestimos_equipamentos (devolvido_por_usuario_id);
