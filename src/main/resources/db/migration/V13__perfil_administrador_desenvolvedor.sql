ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_tipo_de_perfil;
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_tipo_de_perfil
    CHECK (tipo_de_perfil IN ('ADM_DEV', 'ADMINISTRADOR', 'USUARIO'));

ALTER TABLE historico_administracao_usuarios DROP CONSTRAINT ck_historico_administracao_perfil_anterior;
ALTER TABLE historico_administracao_usuarios ADD CONSTRAINT ck_historico_administracao_perfil_anterior
    CHECK (perfil_anterior IS NULL OR perfil_anterior IN ('ADM_DEV', 'ADMINISTRADOR', 'USUARIO'));
ALTER TABLE historico_administracao_usuarios DROP CONSTRAINT ck_historico_administracao_perfil_novo;
ALTER TABLE historico_administracao_usuarios ADD CONSTRAINT ck_historico_administracao_perfil_novo
    CHECK (perfil_novo IS NULL OR perfil_novo IN ('ADM_DEV', 'ADMINISTRADOR', 'USUARIO'));

ALTER TABLE historico_administracao_usuarios DROP CONSTRAINT ck_historico_administracao_tipo;
ALTER TABLE historico_administracao_usuarios ADD CONSTRAINT ck_historico_administracao_tipo CHECK (
    tipo_evento IN (
        'CRIACAO', 'ATIVACAO', 'REEMISSAO_ATIVACAO', 'PROMOCAO_ADMINISTRADOR',
        'REBAIXAMENTO_USUARIO', 'DESATIVACAO', 'REATIVACAO', 'REDEFINICAO_SENHA',
        'ALTERACAO_SENHA', 'CONVERSAO_PERFIL_ADM_DEV', 'ALTERACAO_PERFIL'
    )
);

UPDATE usuarios SET tipo_de_perfil = 'ADM_DEV' WHERE tipo_de_perfil = 'ADMINISTRADOR';

INSERT INTO historico_administracao_usuarios (
    id_usuario, realizado_por_usuario_id, tipo_evento, perfil_anterior, perfil_novo,
    status_anterior, status_novo, justificativa, ocorrido_em
)
SELECT id_usuario, NULL, 'CONVERSAO_PERFIL_ADM_DEV', 'ADMINISTRADOR', 'ADM_DEV',
       status, status, 'Conversão de perfil pela migração V13.', CURRENT_TIMESTAMP
  FROM usuarios WHERE tipo_de_perfil = 'ADM_DEV';
