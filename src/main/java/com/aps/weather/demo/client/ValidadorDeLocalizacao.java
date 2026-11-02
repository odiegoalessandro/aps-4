package com.aps.weather.demo.client;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.exception.DadoInvalidoException;

/**
 * Coordenadas vêm do cadastro, não da requisição. Ainda assim, uma linha
 * corrompida não deve gerar uma URI inválida para a fonte externa.
 */
public final class ValidadorDeLocalizacao {

    private ValidadorDeLocalizacao() {}

    public static void validarCoordenadas(Localizacao local) throws DadoInvalidoException {
        if (local.getLatitude() < -90 || local.getLatitude() > 90) {
            throw new DadoInvalidoException(
                    "latitude inválida para " + local.getCity() + ": " + local.getLatitude());
        }
        if (local.getLongitude() < -180 || local.getLongitude() > 180) {
            throw new DadoInvalidoException(
                    "longitude inválida para " + local.getCity() + ": " + local.getLongitude());
        }
    }
}
