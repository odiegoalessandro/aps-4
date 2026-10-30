-- Cidades eram únicas apenas na busca (findByCityIgnoreCase); o banco aceitava
-- "São Paulo" e "são paulo" como linhas distintas. Colapsa por lower(city), mantendo
-- o menor id de cada grafia normalizada.
CREATE TEMP TABLE cidade_vencedora AS
SELECT lower(city) AS chave, MIN(id) AS vencedora
FROM locations
GROUP BY lower(city);

-- O repontamento pode colocar duas previsões no mesmo (location_id, forecast_at).
-- Suspende o índice único criado na V2 para repontar e deduplicar em seguida.
DROP INDEX IF EXISTS ux_previsao_location_forecast;

UPDATE weather_forecasts
SET location_id = (
    SELECT v.vencedora
    FROM cidade_vencedora v
    JOIN locations l ON l.id = weather_forecasts.location_id
    WHERE v.chave = lower(l.city)
)
WHERE location_id NOT IN (SELECT vencedora FROM cidade_vencedora);

DELETE FROM weather_forecasts
WHERE id NOT IN (
    SELECT MAX(id) FROM weather_forecasts GROUP BY location_id, forecast_at
);

DELETE FROM locations
WHERE id NOT IN (SELECT vencedora FROM cidade_vencedora);

DROP TABLE cidade_vencedora;

CREATE UNIQUE INDEX ux_previsao_location_forecast
    ON weather_forecasts (location_id, forecast_at);

CREATE UNIQUE INDEX ux_location_city_nocase
    ON locations (city COLLATE NOCASE);
