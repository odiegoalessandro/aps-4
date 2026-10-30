package com.aps.weather.demo.client;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteSimuladoTest {

    private static final Localizacao SAO_PAULO =
            new Localizacao("São Paulo", -23.55, -46.63, "America/Sao_Paulo");

    @ParameterizedTest
    @EnumSource(Cenario.class)
    void cadaCenarioGeraUmaPrevisaoValida(Cenario cenario) throws Exception {
        ClienteSimulado cliente = new ClienteSimulado(cenario.name());

        PrevisaoDoCLima previsao = cliente.buscar(SAO_PAULO);

        assertThat(previsao.getLocation()).isSameAs(SAO_PAULO);
        assertThat(previsao.getForecastAt()).isNotNull();
        assertThat(previsao.getUpdatedAt()).isNotNull();
        assertThat(previsao.getTemperature()).isBetween(-90.0, 60.0);
        assertThat(previsao.getApparentTemperature()).isBetween(-120.0, 80.0);
        assertThat(previsao.getHumidity()).isBetween(0, 100);
        assertThat(previsao.getPrecipitation()).isBetween(0.0, 1000.0);
        assertThat(previsao.getWeatherCode()).isEqualTo(cenario.codigoTempo);
        assertThat(previsao.getWindSpeed()).isBetween(0.0, 500.0);
        assertThat(previsao.getWindDirection()).isBetween(0.0, 360.0);
        assertThat(previsao.getPressure()).isBetween(600.0, 1400.0);
        assertThat(previsao.getCloudCover()).isBetween(0.0, 100.0);
    }

    @Test
    void cenarioDeTextoAceitaCaixaEHifen() {
        assertThat(Cenario.deTexto(" tempestade ")).isEqualTo(Cenario.TEMPESTADE);
        assertThat(Cenario.deTexto("frente-fria")).isEqualTo(Cenario.FRENTE_FRIA);
        assertThat(Cenario.deTexto("ONDA_DE_CALOR")).isEqualTo(Cenario.ONDA_DE_CALOR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "chuva-de-meteoro", "desconhecido"})
    void cenarioInvalidoFalhaComMensagemUtil(String texto) {
        assertThatThrownBy(() -> Cenario.deTexto(texto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cenário desconhecido")
                .hasMessageContaining("TEMPESTADE");
    }
}
