-- A V4 adicionou city_normalizada anulável (o SQLite recusa ADD COLUMN NOT NULL),
-- mas a entidade Localizacao declara nullable = false e o índice único trata cada
-- NULL como distinto — sem esta barreira, uma escrita fora do Hibernate poderia
-- deixar a coluna vazia e reabrir a duplicidade de cidades.
CREATE TRIGGER trg_locations_city_normalizada_obrigatoria
BEFORE INSERT ON locations
WHEN NEW.city_normalizada IS NULL
BEGIN
    SELECT RAISE(ABORT, 'city_normalizada deve ser preenchida');
END;
