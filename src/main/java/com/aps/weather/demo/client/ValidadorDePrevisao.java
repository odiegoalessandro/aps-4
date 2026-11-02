package com.aps.weather.demo.client;

import com.aps.weather.demo.exception.DadoInvalidoException;

import java.util.List;

/**
 * Valida o formato das previsões recebidas antes de qualquer conversão de horário.
 * Arrays de horários e temperaturas com tamanhos diferentes são tratados como dado
 * inválido, não como ausência de previsão.
 */
public final class ValidadorDePrevisao {

    private static final double TEMPERATURA_MINIMA = -90;
    private static final double TEMPERATURA_MAXIMA = 60;
    private static final int HORAS_DO_DIA = 24;
    private static final int DIAS_DE_PREVISAO = 7;

    private ValidadorDePrevisao() {}

    public static void validarHoraria(PrevisaoHorariaResposta resposta)
            throws DadoInvalidoException {
        if (resposta == null) {
            throw new DadoInvalidoException("resposta vazia da API");
        }
        PrevisaoHorariaResposta.Horaria horaria = resposta.hourly;
        if (horaria == null) {
            throw new DadoInvalidoException("resposta sem o bloco 'hourly'");
        }
        validarListas("hourly", horaria.time, horaria.temperature2m);
        for (int i = 0; i < horaria.time.size(); i++) {
            validarHorario("hourly.time", horaria.time.get(i));
            validarTemperatura("hourly.temperature_2m", horaria.temperature2m.get(i));
        }
        if (horaria.time.size() != HORAS_DO_DIA) {
            throw new DadoInvalidoException(
                    "quantidade de horários do dia atual difere: esperados " + HORAS_DO_DIA
                            + ", recebidos " + horaria.time.size());
        }
    }

    public static void validarDiaria(PrevisaoDiariaResposta resposta)
            throws DadoInvalidoException {
        if (resposta == null) {
            throw new DadoInvalidoException("resposta vazia da API");
        }
        PrevisaoDiariaResposta.Diaria diaria = resposta.daily;
        if (diaria == null) {
            throw new DadoInvalidoException("resposta sem o bloco 'daily'");
        }
        validarListas("daily", diaria.time, diaria.temperature2mMin);
        if (diaria.time.size() != diaria.temperature2mMax.size()) {
            throw new DadoInvalidoException(
                    "quantidade de datas e temperaturas máximas difere: "
                            + diaria.time.size() + " e " + diaria.temperature2mMax.size());
        }
        for (int i = 0; i < diaria.time.size(); i++) {
            validarHorario("daily.time", diaria.time.get(i));
            double minima = validarTemperatura("daily.temperature_2m_min",
                    diaria.temperature2mMin.get(i));
            double maxima = validarTemperatura("daily.temperature_2m_max",
                    diaria.temperature2mMax.get(i));
            if (minima > maxima) {
                throw new DadoInvalidoException(
                        "temperatura mínima maior que a máxima em " + diaria.time.get(i));
            }
        }
        if (diaria.time.size() != DIAS_DE_PREVISAO) {
            throw new DadoInvalidoException(
                    "quantidade de dias previstos difere: esperados " + DIAS_DE_PREVISAO
                            + ", recebidos " + diaria.time.size());
        }
    }

    private static void validarListas(String bloco, List<String> horarios,
                                      List<Double> temperaturas) throws DadoInvalidoException {
        if (horarios == null || horarios.isEmpty()) {
            throw new DadoInvalidoException("lista de horários vazia em '" + bloco + "'");
        }
        if (temperaturas == null || temperaturas.isEmpty()) {
            throw new DadoInvalidoException("lista de temperaturas vazia em '" + bloco + "'");
        }
        if (horarios.size() != temperaturas.size()) {
            throw new DadoInvalidoException(
                    "quantidade de horários e temperaturas difere em '" + bloco + "': "
                            + horarios.size() + " e " + temperaturas.size());
        }
    }

    private static void validarHorario(String campo, String horario)
            throws DadoInvalidoException {
        if (horario == null || horario.isBlank()) {
            throw new DadoInvalidoException(campo + " ausente");
        }
    }

    private static double validarTemperatura(String campo, Double valor)
            throws DadoInvalidoException {
        if (valor == null) {
            throw new DadoInvalidoException(campo + " ausente");
        }
        if (Double.isNaN(valor) || valor < TEMPERATURA_MINIMA || valor > TEMPERATURA_MAXIMA) {
            throw new DadoInvalidoException(campo + " fora da faixa esperada: " + valor);
        }
        return valor;
    }
}
