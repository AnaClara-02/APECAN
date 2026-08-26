UPDATE usuarios
SET telefone = '55' || telefone
WHERE LENGTH(telefone) IN (10, 11);

UPDATE pacientes
SET telefone = '55' || telefone
WHERE LENGTH(telefone) IN (10, 11);

UPDATE voluntarios
SET telefone = '55' || telefone
WHERE LENGTH(telefone) IN (10, 11);

ALTER TABLE usuarios
    DROP CONSTRAINT ck_usuarios_telefone,
    ADD CONSTRAINT ck_usuarios_telefone CHECK (telefone ~ '^55[0-9]{10,11}$');

ALTER TABLE pacientes
    DROP CONSTRAINT ck_pacientes_telefone,
    ADD CONSTRAINT ck_pacientes_telefone CHECK (telefone ~ '^55[0-9]{10,11}$');

ALTER TABLE voluntarios
    DROP CONSTRAINT ck_voluntarios_telefone,
    ADD CONSTRAINT ck_voluntarios_telefone CHECK (telefone ~ '^55[0-9]{10,11}$');
