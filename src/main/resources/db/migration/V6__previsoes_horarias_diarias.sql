-- Previsões horárias e diárias persistidas (ADR-0003). A leitura é read-through:
-- o snapshot é substituído por completo quando a fonte responde ou o TTL vence.
--
-- "id INTEGER" é obrigatório no SQLite para ser alias de rowid e auto-incrementar
-- (ver docs/adr/0002-dialeto-sqlite.md). Os índices únicos garantem que uma
-- substituição concorrente não duplique horário nem data.
CREATE TABLE hourly_forecasts (
    id           INTEGER   NOT NULL,
    location_id  BIGINT    NOT NULL,
    forecast_at  TIMESTAMP NOT NULL,
    temperature  FLOAT     NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (location_id) REFERENCES locations (id)
);

CREATE UNIQUE INDEX ux_hourly_location_forecast_at
    ON hourly_forecasts (location_id, forecast_at);

CREATE TABLE daily_forecasts (
    id               INTEGER   NOT NULL,
    location_id      BIGINT    NOT NULL,
    forecast_date    DATE      NOT NULL,
    min_temperature  FLOAT     NOT NULL,
    max_temperature  FLOAT     NOT NULL,
    updated_at       TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (location_id) REFERENCES locations (id)
);

CREATE UNIQUE INDEX ux_daily_location_forecast_date
    ON daily_forecasts (location_id, forecast_date);
