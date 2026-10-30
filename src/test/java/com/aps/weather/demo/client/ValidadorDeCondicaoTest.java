package com.aps.weather.demo.client;

import com.aps.weather.demo.service.DadoInvalidoException;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorDeCondicaoTest {

    @Test
    void valoresDentroDaFaixaSaoAceitos() {
        assertThatCode(() -> ValidadorDeCondicao.validar(valida()))
                .doesNotThrowAnyException();
    }

    @Test
    void valoresNosLimitesSaoAceitos() {
        CondicaoAtual noMinimo = valida();
        noMinimo.temperatura = -90.0;
        noMinimo.sensacaoTermica = -120.0;
        noMinimo.umidade = 0;
        noMinimo.precipitacao = 0.0;
        noMinimo.codigoTempo = 0;
        noMinimo.vento = 0.0;
        noMinimo.direcaoVento = 0.0;
        noMinimo.pressao = 600.0;
        noMinimo.nuvens = 0.0;

        CondicaoAtual noMaximo = valida();
        noMaximo.temperatura = 60.0;
        noMaximo.sensacaoTermica = 80.0;
        noMaximo.umidade = 100;
        noMaximo.precipitacao = 1000.0;
        noMaximo.codigoTempo = 99;
        noMaximo.vento = 500.0;
        noMaximo.direcaoVento = 360.0;
        noMaximo.pressao = 1400.0;
        noMaximo.nuvens = 100.0;

        assertThatCode(() -> ValidadorDeCondicao.validar(noMinimo))
                .doesNotThrowAnyException();
        assertThatCode(() -> ValidadorDeCondicao.validar(noMaximo))
                .doesNotThrowAnyException();
    }

    @Test
    void blocoAusente() {
        assertThatThrownBy(() -> ValidadorDeCondicao.validar(null))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("current");
    }

    @Test
    void horarioAusenteOuEmBranco() {
        CondicaoAtual semHorario = valida();
        semHorario.time = null;
        assertThatThrownBy(() -> ValidadorDeCondicao.validar(semHorario))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("time");

        CondicaoAtual horarioEmBranco = valida();
        horarioEmBranco.time = "   ";
        assertThatThrownBy(() -> ValidadorDeCondicao.validar(horarioEmBranco))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("time");
    }

    @Test
    void campoNumericoAusente() {
        CondicaoAtual c = valida();
        c.umidade = null;
        assertThatThrownBy(() -> ValidadorDeCondicao.validar(c))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("relative_humidity_2m");
    }

    @Test
    void valorNaoNumerico() {
        CondicaoAtual c = valida();
        c.temperatura = Double.NaN;
        assertThatThrownBy(() -> ValidadorDeCondicao.validar(c))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("temperature_2m");
    }

    @Test
    void temperaturaForaDaFaixa() {
        forcaFora("temperature_2m", c -> c.temperatura = -90.1);
        forcaFora("temperature_2m", c -> c.temperatura = 60.1);
    }

    @Test
    void sensacaoTermicaForaDaFaixa() {
        forcaFora("apparent_temperature", c -> c.sensacaoTermica = -120.1);
        forcaFora("apparent_temperature", c -> c.sensacaoTermica = 80.1);
    }

    @Test
    void umidadeForaDaFaixa() {
        forcaFora("relative_humidity_2m", c -> c.umidade = -1);
        forcaFora("relative_humidity_2m", c -> c.umidade = 101);
    }

    @Test
    void precipitacaoForaDaFaixa() {
        forcaFora("precipitation", c -> c.precipitacao = -0.1);
        forcaFora("precipitation", c -> c.precipitacao = 1000.1);
    }

    @Test
    void codigoTempoForaDaFaixa() {
        forcaFora("weather_code", c -> c.codigoTempo = -1);
        forcaFora("weather_code", c -> c.codigoTempo = 100);
    }

    @Test
    void ventoForaDaFaixa() {
        forcaFora("wind_speed_10m", c -> c.vento = -0.1);
        forcaFora("wind_speed_10m", c -> c.vento = 500.1);
    }

    @Test
    void direcaoDoVentoForaDaFaixa() {
        forcaFora("wind_direction_10m", c -> c.direcaoVento = -0.1);
        forcaFora("wind_direction_10m", c -> c.direcaoVento = 360.1);
    }

    @Test
    void pressaoForaDaFaixa() {
        forcaFora("pressure_msl", c -> c.pressao = 599.9);
        forcaFora("pressure_msl", c -> c.pressao = 1400.1);
    }

    @Test
    void nuvensForaDaFaixa() {
        forcaFora("cloud_cover", c -> c.nuvens = -0.1);
        forcaFora("cloud_cover", c -> c.nuvens = 100.1);
    }

    private static void forcaFora(String campo, Consumer<CondicaoAtual> mutacao) {
        CondicaoAtual c = valida();
        mutacao.accept(c);
        assertThatThrownBy(() -> ValidadorDeCondicao.validar(c))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining(campo);
    }

    private static CondicaoAtual valida() {
        CondicaoAtual c = new CondicaoAtual();
        c.time = "2026-10-30T15:00";
        c.temperatura = 20.0;
        c.sensacaoTermica = 19.0;
        c.umidade = 60;
        c.precipitacao = 0.0;
        c.codigoTempo = 3;
        c.vento = 10.0;
        c.direcaoVento = 180.0;
        c.pressao = 1013.0;
        c.nuvens = 50.0;
        return c;
    }
}
