package com.aps.weather.demo.api.dto;

import com.aps.weather.demo.service.ResultadoPrevisaoDiaria;

import java.util.List;

public record PrevisaoDiariaResponse(
        String timezone,
        List<PrevisaoDoDia> forecast
) {

    public record PrevisaoDoDia(String date, double minTemperature, double maxTemperature) {
    }

    public static PrevisaoDiariaResponse from(ResultadoPrevisaoDiaria resultado) {
        return new PrevisaoDiariaResponse(
                resultado.fuso().getId(),
                resultado.previsoes().stream()
                        .map(previsao -> new PrevisaoDoDia(
                                previsao.getForecastDate().toString(),
                                previsao.getMinTemperature(),
                                previsao.getMaxTemperature()))
                        .toList());
    }
}
