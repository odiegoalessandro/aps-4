-- A comparação sem caixa do SQLite (COLLATE NOCASE e lower()) só dobra ASCII:
-- "Brasília" e "BRASÍLIA" continuavam sendo cidades distintas e a busca por
-- "SÃO PAULO" não achava a linha semeada como "São Paulo". A coluna abaixo guarda
-- a forma normalizada (sem acentos, em minúsculas), usada nas buscas e protegida
-- por índice único. Novos registros recebem o valor de NomeDeCidade.normalizar();
-- o backfill abaixo só vale para as linhas legadas.
--
-- A coluna fica anulável porque o SQLite não permite adicionar NOT NULL a uma
-- tabela existente; a V5 instala um trigger que recusa inserções sem valor.
ALTER TABLE locations ADD COLUMN city_normalizada TEXT;

-- Backfill das linhas legadas: os replace() removem os acentos (maiúsculos e
-- minúsculos) e o lower()/trim() cobrem caixa ASCII e espaços das pontas.
--
-- Limite conhecido: o SQLite não tem NFD, então esta tabela de replace cobre o
-- conjunto Latin-1, não a decomposição completa que o Java usa. Letras fora do
-- conjunto (ex.: "Ö" vira "O" no Java, mas continua "Ö" aqui) gerariam formas
-- diferentes das novas. A tabela legada só contém as cinco cidades fixas do
-- APIClima-usoBasico, todas cobertas, e não há endpoint de criação de cidade.
UPDATE locations
SET city_normalizada = lower(trim(
    replace(replace(replace(replace(replace(replace(replace(replace(
    replace(replace(replace(replace(replace(replace(replace(replace(
    replace(replace(replace(replace(replace(replace(replace(replace(
    replace(replace(replace(replace(city,
    'á','a'), 'Á','a'), 'à','a'), 'À','a'),
    'â','a'), 'Â','a'), 'ã','a'), 'Ã','a'),
    'é','e'), 'É','e'), 'ê','e'), 'Ê','e'),
    'í','i'), 'Í','i'),
    'ó','o'), 'Ó','o'), 'ô','o'), 'Ô','o'),
    'õ','o'), 'Õ','o'),
    'ú','u'), 'Ú','u'), 'ü','u'), 'Ü','u'),
    'ç','c'), 'Ç','c'),
    'ñ','n'), 'Ñ','n')
));

-- Normalizar pode colapsar grafias que se diferenciam só pelo acento
-- ("São Paulo" e "Sao Paulo"). Repete o padrão da V3: mantém o menor id de cada
-- chave, repointa as previsões do perdedor e deduplica o que colidir.
DROP INDEX IF EXISTS ux_location_city_nocase;
DROP INDEX IF EXISTS ux_previsao_location_forecast;

CREATE TEMP TABLE cidade_normalizada_vencedora AS
SELECT city_normalizada AS chave, MIN(id) AS vencedora
FROM locations
GROUP BY city_normalizada;

UPDATE weather_forecasts
SET location_id = (
    SELECT v.vencedora
    FROM cidade_normalizada_vencedora v
    JOIN locations l ON l.id = weather_forecasts.location_id
    WHERE v.chave = l.city_normalizada
)
WHERE location_id NOT IN (SELECT vencedora FROM cidade_normalizada_vencedora)
  AND location_id IN (SELECT id FROM locations);

DELETE FROM weather_forecasts
WHERE id NOT IN (
    SELECT MAX(id) FROM weather_forecasts GROUP BY location_id, forecast_at
);

DELETE FROM locations
WHERE id NOT IN (SELECT vencedora FROM cidade_normalizada_vencedora);

DROP TABLE cidade_normalizada_vencedora;

CREATE UNIQUE INDEX ux_previsao_location_forecast
    ON weather_forecasts (location_id, forecast_at);

CREATE UNIQUE INDEX ux_location_city_normalizada
    ON locations (city_normalizada);
