package com.aps.weather.demo.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CondicaoClimaticaTest {

    @ParameterizedTest
    @CsvSource({
            "0, CEU_LIMPO",
            "1, PREDOMINANTEMENTE_LIMPO",
            "2, PARCIALMENTE_NUBLADO",
            "3, NUBLADO",
            "45, NEBLINA",
            "51, GAROA_FRACA",
            "61, CHUVA_FRACA",
            "71, NEVE_FRACA",
            "80, PANCADAS_DE_CHUVA_FRACAS",
            "95, TEMPESTADE"
    })
    void codigosConhecidosMapeiamParaACondicaoEsperada(int codigo, String esperado) {
        assertThat(CondicaoClimatica.deCodigo(codigo).name()).isEqualTo(esperado);
    }

    @Test
    void codigoDesconhecidoCaiNoFallback() {
        assertThat(CondicaoClimatica.deCodigo(7)).isSameAs(CondicaoClimatica.DESCONHECIDO);
        assertThat(CondicaoClimatica.deCodigo(-1)).isSameAs(CondicaoClimatica.DESCONHECIDO);
        assertThat(CondicaoClimatica.deCodigo(1234)).isSameAs(CondicaoClimatica.DESCONHECIDO);
        assertThat(CondicaoClimatica.DESCONHECIDO.getDescricao()).isEqualTo("Desconhecido");
    }

    @Test
    void todaCondicaoTemDescricaoNaoVazia() {
        for (CondicaoClimatica condicao : CondicaoClimatica.values()) {
            assertThat(condicao.getDescricao()).isNotBlank();
        }
    }
}
