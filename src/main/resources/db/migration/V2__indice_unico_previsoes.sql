-- A deduplicação existia só no código (existsByLocationAndForecastAt): duas coletas
-- concorrentes para o mesmo horário podiam gravar as duas. A constraint passa a ser a
-- garantia; o flag app.weather.collection.pular-repetidos continua evitando a chamada.
--
-- Antes de criar o índice, colapsa duplicados mantendo o maior id de cada par.
DELETE FROM weather_forecasts
WHERE id NOT IN (
    SELECT MAX(id) FROM weather_forecasts GROUP BY location_id, forecast_at
);

-- Índice composto: além de garantir a unicidade, atende a busca da última previsão e do
-- histórico por localização, então não há índice redundante a criar.
CREATE UNIQUE INDEX ux_previsao_location_forecast
    ON weather_forecasts (location_id, forecast_at);
