package com.aps.weather.demo.service;

import com.aps.weather.demo.domain.PrevisaoDoCLima;

import java.util.List;

/** Resultado de uma coleta em lote: o que foi gravado e o que falhou. */
public record ResultadoColeta(
        List<PrevisaoDoCLima> gravadas,
        List<FalhaColeta> falhas
) {
}
