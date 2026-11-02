package com.aps.weather.demo.api.dto;

import com.aps.weather.demo.service.ResultadoPrevisaoHoraria;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record PrevisaoHorariaResponse(
        String date,
        String timezone,
        List<TemperaturaHoraria> temperatures
) {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public record TemperaturaHoraria(String time, double temperature) {
    }

    public static PrevisaoHorariaResponse from(ResultadoPrevisaoHoraria resultado) {
        return new PrevisaoHorariaResponse(
                resultado.data().toString(),
                resultado.fuso().getId(),
                resultado.previsoes().stream()
                        .map(previsao -> new TemperaturaHoraria(
                                HORA.format(previsao.getForecastAt().atZone(resultado.fuso())),
                                previsao.getTemperature()))
                        .toList());
    }
}
