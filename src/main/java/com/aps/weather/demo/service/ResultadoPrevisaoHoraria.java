package com.aps.weather.demo.service;

import com.aps.weather.demo.domain.PrevisaoHoraria;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public record ResultadoPrevisaoHoraria(
        LocalDate data,
        ZoneId fuso,
        List<PrevisaoHoraria> previsoes
) {
}
