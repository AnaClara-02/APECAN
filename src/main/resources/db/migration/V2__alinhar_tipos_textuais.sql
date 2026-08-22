-- Alinha os tipos PostgreSQL com o mapeamento JPA padrão de String.
-- As restrições CHECK existentes continuam garantindo o formato e o tamanho.

ALTER TABLE usuarios
    ALTER COLUMN cpf TYPE VARCHAR(11);

ALTER TABLE pacientes
    ALTER COLUMN cpf TYPE VARCHAR(11);

ALTER TABLE voluntarios
    ALTER COLUMN cpf TYPE VARCHAR(11);

ALTER TABLE tokens_redefinicao_senha
    ALTER COLUMN token_hash TYPE VARCHAR(64);
