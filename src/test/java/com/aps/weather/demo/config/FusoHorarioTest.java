package com.aps.weather.demo.config;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.exception.DadoInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FusoHorarioTest {

    private final FusoHorario fuso = new FusoHorario("America/Sao_Paulo");

    @Test
    void usaOTimezoneDaLocalizacao() throws Exception {
        Localizacao local = new Localizacao("Tóquio", 35.68, 139.69, "Asia/Tokyo");

        assertThat(fuso.zona(local)).isEqualTo(ZoneId.of("Asia/Tokyo"));
        assertThat(fuso.id(local)).isEqualTo("Asia/Tokyo");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void timezoneAusenteUsaOPadrao(String timezone) throws Exception {
        Localizacao local = new Localizacao("Sem fuso", -23.5, -46.6, timezone);

        assertThat(fuso.id(local)).isEqualTo("America/Sao_Paulo");
    }

    @Test
    void timezoneInvalidaEhErroDeDado() {
        Localizacao local = new Localizacao("Marte", -23.5, -46.6, "Marte/Olympus");

        assertThatThrownBy(() -> fuso.zona(local))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("timezone inválida")
                .hasMessageContaining("Marte");
    }

    @Test
    void padraoVazioNoConstrutorVoltaParaOBrasil() throws Exception {
        FusoHorario semPadrao = new FusoHorario("  ");
        Localizacao local = new Localizacao("Sem fuso", -23.5, -46.6, null);

        assertThat(semPadrao.id(local)).isEqualTo("America/Sao_Paulo");
    }
}
