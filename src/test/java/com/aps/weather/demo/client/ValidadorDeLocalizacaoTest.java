package com.aps.weather.demo.client;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.exception.DadoInvalidoException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorDeLocalizacaoTest {

    @Test
    void aceitaCoordenadasNosLimites() {
        assertThatCode(() -> ValidadorDeLocalizacao.validarCoordenadas(
                new Localizacao("Polo", 90, -180, "UTC"))).doesNotThrowAnyException();
        assertThatCode(() -> ValidadorDeLocalizacao.validarCoordenadas(
                new Localizacao("Polo", -90, 180, "UTC"))).doesNotThrowAnyException();
    }

    @Test
    void recusaLatitudeForaDaFaixa() {
        assertThatThrownBy(() -> ValidadorDeLocalizacao.validarCoordenadas(
                new Localizacao("Fora", 91, 0, "UTC")))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("latitude inválida");
    }

    @Test
    void recusaLongitudeForaDaFaixa() {
        assertThatThrownBy(() -> ValidadorDeLocalizacao.validarCoordenadas(
                new Localizacao("Fora", 0, -181, "UTC")))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("longitude inválida");
    }
}
