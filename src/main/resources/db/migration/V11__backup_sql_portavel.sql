-- Permite recuperar dados operacionais sem transportar contas de acesso.
-- A autoria original e preservada como texto; novos registros continuam
-- vinculados ao usuario autenticado normalmente.

ALTER TABLE pacientes
    ALTER COLUMN criado_por_usuario_id DROP NOT NULL,
    ADD COLUMN criado_por_nome_historico VARCHAR(150),
    ADD COLUMN atualizado_por_nome_historico VARCHAR(150),
    ADD COLUMN importado_em TIMESTAMP;
UPDATE pacientes p SET criado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = p.criado_por_usuario_id;
UPDATE pacientes p SET atualizado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = p.atualizado_por_usuario_id;
ALTER TABLE pacientes ADD CONSTRAINT ck_pacientes_autoria
    CHECK (criado_por_usuario_id IS NOT NULL OR criado_por_nome_historico IS NOT NULL);

ALTER TABLE historico_status_paciente
    ALTER COLUMN alterado_por_usuario_id DROP NOT NULL,
    ADD COLUMN alterado_por_nome_historico VARCHAR(150),
    ADD COLUMN importado_em TIMESTAMP;
UPDATE historico_status_paciente h SET alterado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = h.alterado_por_usuario_id;
ALTER TABLE historico_status_paciente ADD CONSTRAINT ck_historico_status_autoria
    CHECK (alterado_por_usuario_id IS NOT NULL OR alterado_por_nome_historico IS NOT NULL);

ALTER TABLE voluntarios
    ALTER COLUMN criado_por_usuario_id DROP NOT NULL,
    ADD COLUMN criado_por_nome_historico VARCHAR(150),
    ADD COLUMN atualizado_por_nome_historico VARCHAR(150),
    ADD COLUMN importado_em TIMESTAMP;
UPDATE voluntarios v SET criado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = v.criado_por_usuario_id;
UPDATE voluntarios v SET atualizado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = v.atualizado_por_usuario_id;
ALTER TABLE voluntarios ADD CONSTRAINT ck_voluntarios_autoria
    CHECK (criado_por_usuario_id IS NOT NULL OR criado_por_nome_historico IS NOT NULL);

ALTER TABLE categorias_equipamentos
    ALTER COLUMN criado_por_usuario_id DROP NOT NULL,
    ADD COLUMN criado_por_nome_historico VARCHAR(150),
    ADD COLUMN importado_em TIMESTAMP;
UPDATE categorias_equipamentos c SET criado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = c.criado_por_usuario_id;
ALTER TABLE categorias_equipamentos ADD CONSTRAINT ck_categorias_autoria
    CHECK (criado_por_usuario_id IS NOT NULL OR criado_por_nome_historico IS NOT NULL);

ALTER TABLE equipamentos
    ALTER COLUMN criado_por_usuario_id DROP NOT NULL,
    ADD COLUMN criado_por_nome_historico VARCHAR(150),
    ADD COLUMN atualizado_por_nome_historico VARCHAR(150),
    ADD COLUMN importado_em TIMESTAMP;
UPDATE equipamentos e SET criado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = e.criado_por_usuario_id;
UPDATE equipamentos e SET atualizado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = e.atualizado_por_usuario_id;
ALTER TABLE equipamentos ADD CONSTRAINT ck_equipamentos_autoria
    CHECK (criado_por_usuario_id IS NOT NULL OR criado_por_nome_historico IS NOT NULL);

ALTER TABLE emprestimos_equipamentos
    ALTER COLUMN registrado_por_usuario_id DROP NOT NULL,
    ADD COLUMN registrado_por_nome_historico VARCHAR(150),
    ADD COLUMN devolvido_por_nome_historico VARCHAR(150),
    ADD COLUMN importado_em TIMESTAMP;
UPDATE emprestimos_equipamentos e SET registrado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = e.registrado_por_usuario_id;
UPDATE emprestimos_equipamentos e SET devolvido_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = e.devolvido_por_usuario_id;
ALTER TABLE emprestimos_equipamentos DROP CONSTRAINT ck_emprestimos_devolucao_detalhes;
ALTER TABLE emprestimos_equipamentos ADD CONSTRAINT ck_emprestimos_autoria
    CHECK (registrado_por_usuario_id IS NOT NULL OR registrado_por_nome_historico IS NOT NULL);
ALTER TABLE emprestimos_equipamentos ADD CONSTRAINT ck_emprestimos_devolucao_detalhes CHECK (
    (data_devolucao IS NULL AND devolvido_por_usuario_id IS NULL
        AND devolvido_por_nome_historico IS NULL AND estado_conservacao_devolucao IS NULL)
    OR
    (data_devolucao IS NOT NULL
        AND (devolvido_por_usuario_id IS NOT NULL OR devolvido_por_nome_historico IS NOT NULL)
        AND estado_conservacao_devolucao IN ('NOVO', 'BOM', 'REGULAR', 'DANIFICADO'))
);

ALTER TABLE doacoes
    ALTER COLUMN registrado_por_usuario_id DROP NOT NULL,
    ADD COLUMN registrado_por_nome_historico VARCHAR(150),
    ADD COLUMN importado_em TIMESTAMP;
UPDATE doacoes d SET registrado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = d.registrado_por_usuario_id;
ALTER TABLE doacoes ADD CONSTRAINT ck_doacoes_autoria
    CHECK (registrado_por_usuario_id IS NOT NULL OR registrado_por_nome_historico IS NOT NULL);

ALTER TABLE movimentacoes_financeiras
    ALTER COLUMN registrado_por_usuario_id DROP NOT NULL,
    ADD COLUMN registrado_por_nome_historico VARCHAR(150),
    ADD COLUMN importado_em TIMESTAMP;
UPDATE movimentacoes_financeiras m SET registrado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = m.registrado_por_usuario_id;
ALTER TABLE movimentacoes_financeiras ADD CONSTRAINT ck_movimentacoes_autoria
    CHECK (registrado_por_usuario_id IS NOT NULL OR registrado_por_nome_historico IS NOT NULL);

ALTER TABLE despesas
    ALTER COLUMN registrado_por_usuario_id DROP NOT NULL,
    ADD COLUMN registrado_por_nome_historico VARCHAR(150),
    ADD COLUMN importado_em TIMESTAMP;
UPDATE despesas d SET registrado_por_nome_historico = u.nome FROM usuarios u WHERE u.id_usuario = d.registrado_por_usuario_id;
ALTER TABLE despesas ADD CONSTRAINT ck_despesas_autoria
    CHECK (registrado_por_usuario_id IS NOT NULL OR registrado_por_nome_historico IS NOT NULL);

ALTER TABLE doacao_voluntarios ADD COLUMN importado_em TIMESTAMP;

-- Mantem o nome como fotografia historica, mesmo se a conta mudar depois.
CREATE FUNCTION preencher_autoria_historica() RETURNS trigger AS $$
BEGIN
    IF TG_ARGV[0] IS NOT NULL AND NEW.criado_por_nome_historico IS NULL AND NEW.criado_por_usuario_id IS NOT NULL THEN
        SELECT nome INTO NEW.criado_por_nome_historico FROM usuarios WHERE id_usuario = NEW.criado_por_usuario_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_pacientes_autoria BEFORE INSERT ON pacientes FOR EACH ROW EXECUTE FUNCTION preencher_autoria_historica('criado');
CREATE TRIGGER trg_voluntarios_autoria BEFORE INSERT ON voluntarios FOR EACH ROW EXECUTE FUNCTION preencher_autoria_historica('criado');
CREATE TRIGGER trg_categorias_autoria BEFORE INSERT ON categorias_equipamentos FOR EACH ROW EXECUTE FUNCTION preencher_autoria_historica('criado');
CREATE TRIGGER trg_equipamentos_autoria BEFORE INSERT ON equipamentos FOR EACH ROW EXECUTE FUNCTION preencher_autoria_historica('criado');

CREATE FUNCTION preencher_autoria_registro() RETURNS trigger AS $$
BEGIN
    IF NEW.registrado_por_nome_historico IS NULL AND NEW.registrado_por_usuario_id IS NOT NULL THEN
        SELECT nome INTO NEW.registrado_por_nome_historico FROM usuarios WHERE id_usuario = NEW.registrado_por_usuario_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_emprestimos_autoria BEFORE INSERT ON emprestimos_equipamentos FOR EACH ROW EXECUTE FUNCTION preencher_autoria_registro();
CREATE TRIGGER trg_doacoes_autoria BEFORE INSERT ON doacoes FOR EACH ROW EXECUTE FUNCTION preencher_autoria_registro();
CREATE TRIGGER trg_movimentacoes_autoria BEFORE INSERT ON movimentacoes_financeiras FOR EACH ROW EXECUTE FUNCTION preencher_autoria_registro();
CREATE TRIGGER trg_despesas_autoria BEFORE INSERT ON despesas FOR EACH ROW EXECUTE FUNCTION preencher_autoria_registro();

CREATE FUNCTION preencher_autoria_status() RETURNS trigger AS $$
BEGIN
    IF NEW.alterado_por_nome_historico IS NULL AND NEW.alterado_por_usuario_id IS NOT NULL THEN
        SELECT nome INTO NEW.alterado_por_nome_historico FROM usuarios WHERE id_usuario = NEW.alterado_por_usuario_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_historico_status_autoria BEFORE INSERT ON historico_status_paciente FOR EACH ROW EXECUTE FUNCTION preencher_autoria_status();

CREATE FUNCTION preencher_autoria_atualizacao() RETURNS trigger AS $$
BEGIN
    IF NEW.atualizado_por_usuario_id IS NOT NULL
       AND NEW.atualizado_por_usuario_id IS DISTINCT FROM OLD.atualizado_por_usuario_id THEN
        SELECT nome INTO NEW.atualizado_por_nome_historico FROM usuarios WHERE id_usuario = NEW.atualizado_por_usuario_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_pacientes_autoria_atualizacao BEFORE UPDATE ON pacientes FOR EACH ROW EXECUTE FUNCTION preencher_autoria_atualizacao();
CREATE TRIGGER trg_voluntarios_autoria_atualizacao BEFORE UPDATE ON voluntarios FOR EACH ROW EXECUTE FUNCTION preencher_autoria_atualizacao();
CREATE TRIGGER trg_equipamentos_autoria_atualizacao BEFORE UPDATE ON equipamentos FOR EACH ROW EXECUTE FUNCTION preencher_autoria_atualizacao();

CREATE FUNCTION preencher_autoria_devolucao() RETURNS trigger AS $$
BEGIN
    IF NEW.devolvido_por_usuario_id IS NOT NULL
       AND NEW.devolvido_por_usuario_id IS DISTINCT FROM OLD.devolvido_por_usuario_id THEN
        SELECT nome INTO NEW.devolvido_por_nome_historico FROM usuarios WHERE id_usuario = NEW.devolvido_por_usuario_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_emprestimos_autoria_devolucao BEFORE UPDATE ON emprestimos_equipamentos FOR EACH ROW EXECUTE FUNCTION preencher_autoria_devolucao();
