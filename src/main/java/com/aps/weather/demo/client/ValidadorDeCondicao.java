package com.aps.weather.demo.client;

import com.aps.weather.demo.exception.DadoInvalidoException;

public final class ValidadorDeCondicao {

    private ValidadorDeCondicao() {}

    public static void validar(CondicaoAtual c) throws DadoInvalidoException {
        if (c == null) {
            throw new DadoInvalidoException("resposta sem o bloco 'current'");
        }
        if (c.time == null || c.time.isBlank()) {
            throw new DadoInvalidoException("campo 'time' ausente");
        }
        faixa("temperature_2m", c.temperatura, -90, 60);
        faixa("apparent_temperature", c.sensacaoTermica, -120, 80);
        faixa("relative_humidity_2m", c.umidade, 0, 100);
        faixa("precipitation", c.precipitacao, 0, 1000);
        faixa("weather_code", c.codigoTempo, 0, 99);
        faixa("wind_speed_10m", c.vento, 0, 500);
        faixa("wind_direction_10m", c.direcaoVento, 0, 360);
        faixa("pressure_msl", c.pressao, 600, 1400);
        faixa("cloud_cover", c.nuvens, 0, 100);
    }

    private static void faixa(String nome, Number valor, double min, double max)
            throws DadoInvalidoException {
        if (valor == null) {
            throw new DadoInvalidoException(nome + " ausente");
        }
        double v = valor.doubleValue();
        if (Double.isNaN(v) || v < min || v > max) {
            throw new DadoInvalidoException(nome + " fora da faixa esperada: " + v);
        }
    }
}
