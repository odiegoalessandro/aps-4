package com.aps.weather.demo.service;

import com.aps.weather.demo.domain.PrevisaoDiaria;

import java.time.ZoneId;
import java.util.List;

public record ResultadoPrevisaoDiaria(
        ZoneId fuso,
        List<PrevisaoDiaria> previsoes
) {
}
