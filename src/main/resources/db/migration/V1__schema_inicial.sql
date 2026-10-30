-- Schema inicial, espelhando o DDL que o Hibernate gerava com ddl-auto=update.
-- "id INTEGER" é obrigatório no SQLite: só essa grafia exata é alias de rowid e
-- portanto auto-incrementa (ver ApsSQLiteDialect).
CREATE TABLE locations (
    id       INTEGER      NOT NULL,
    city     VARCHAR(150) NOT NULL,
    latitude FLOAT        NOT NULL,
    longitude FLOAT       NOT NULL,
    timezone VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE weather_forecasts (
    id                   INTEGER   NOT NULL,
    location_id          BIGINT    NOT NULL,
    forecast_at          TIMESTAMP NOT NULL,
    temperature          FLOAT     NOT NULL,
    apparent_temperature FLOAT     NOT NULL,
    humidity             INTEGER   NOT NULL,
    precipitation        FLOAT     NOT NULL,
    weather_code         INTEGER   NOT NULL,
    wind_speed           FLOAT     NOT NULL,
    wind_direction       FLOAT     NOT NULL,
    pressure             FLOAT     NOT NULL,
    cloud_cover          FLOAT     NOT NULL,
    updated_at           TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (location_id) REFERENCES locations (id)
);
