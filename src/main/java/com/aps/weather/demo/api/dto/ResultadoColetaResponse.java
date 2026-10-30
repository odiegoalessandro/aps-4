package com.aps.weather.demo.api.dto;

import com.aps.weather.demo.service.FalhaColeta;
import com.aps.weather.demo.service.ResultadoColeta;

import java.util.List;

public record ResultadoColetaResponse(
        List<PrevisaoResponse> gravadas,
        List<FalhaColeta> falhas
) {

    public static ResultadoColetaResponse from(ResultadoColeta resultado) {
        return new ResultadoColetaResponse(
                resultado.gravadas().stream().map(PrevisaoResponse::from).toList(),
                resultado.falhas());
    }
}
