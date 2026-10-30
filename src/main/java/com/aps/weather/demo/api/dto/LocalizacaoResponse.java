package com.aps.weather.demo.api.dto;

import com.aps.weather.demo.domain.Localizacao;

public record LocalizacaoResponse(
        Long id,
        String city,
        double latitude,
        double longitude,
        String timezone
) {

    public static LocalizacaoResponse from(Localizacao localizacao) {
        return new LocalizacaoResponse(
                localizacao.getId(),
                localizacao.getCity(),
                localizacao.getLatitude(),
                localizacao.getLongitude(),
                localizacao.getTimezone());
    }
}
