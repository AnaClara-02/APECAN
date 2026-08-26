ALTER TABLE historico_administracao_usuarios
    DROP CONSTRAINT ck_historico_administracao_tipo;

ALTER TABLE historico_administracao_usuarios
    ADD CONSTRAINT ck_historico_administracao_tipo CHECK (
        tipo_evento IN (
            'CRIACAO',
            'ATIVACAO',
            'REEMISSAO_ATIVACAO',
            'PROMOCAO_ADMINISTRADOR',
            'REBAIXAMENTO_USUARIO',
            'DESATIVACAO',
            'REATIVACAO',
            'REDEFINICAO_SENHA',
            'ALTERACAO_SENHA'
        )
    );
