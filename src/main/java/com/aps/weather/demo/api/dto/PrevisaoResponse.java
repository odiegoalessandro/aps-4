package com.aps.weather.demo.api.dto;

import com.aps.weather.demo.domain.CondicaoClimatica;
import com.aps.weather.demo.domain.PrevisaoDoCLima;

import java.time.Instant;

public record PrevisaoResponse(
        Long id,
        String city,
        Instant forecastAt,
        double temperature,
        double apparentTemperature,
        int humidity,
        double precipitation,
        int weatherCode,
        String condition,
        String conditionDescription,
        double windSpeed,
        double windDirection,
        double pressure,
        double cloudCover,
        Instant updatedAt
) {

    public static PrevisaoResponse from(PrevisaoDoCLima previsao) {
        CondicaoClimatica condicao = CondicaoClimatica.deCodigo(previsao.getWeatherCode());
        return new PrevisaoResponse(
                previsao.getId(),
                previsao.getLocation().getCity(),
                previsao.getForecastAt(),
                previsao.getTemperature(),
                previsao.getApparentTemperature(),
                previsao.getHumidity(),
                previsao.getPrecipitation(),
                previsao.getWeatherCode(),
                condicao.name(),
                condicao.getDescricao(),
                previsao.getWindSpeed(),
                previsao.getWindDirection(),
                previsao.getPressure(),
                previsao.getCloudCover(),
                previsao.getUpdatedAt());
    }
}
