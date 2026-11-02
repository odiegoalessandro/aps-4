package com.aps.weather.demo.client;

import com.aps.weather.demo.config.FusoHorario;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteSimuladoTest {

    private static final Localizacao SAO_PAULO =
            new Localizacao("São Paulo", -23.55, -46.63, "America/Sao_Paulo");

    private static final FusoHorario FUSO = new FusoHorario("America/Sao_Paulo");
    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-10-30T18:00:00Z"), ZoneOffset.UTC);
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    @ParameterizedTest
    @EnumSource(Cenario.class)
    void cadaCenarioGeraUmaPrevisaoValida(Cenario cenario) throws Exception {
        ClienteSimulado cliente = novoCliente(cenario);

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

    @ParameterizedTest
    @EnumSource(Cenario.class)
    void simula24HorasDoDiaAtualEmOrdem(Cenario cenario) throws Exception {
        ClienteSimulado cliente = novoCliente(cenario);

        List<PrevisaoHoraria> previsoes = cliente.buscarHorarias(SAO_PAULO);

        assertThat(previsoes).hasSize(24);
        assertThat(previsoes).extracting(PrevisaoHoraria::getForecastAt).isSorted()
                .doesNotHaveDuplicates();
        assertThat(previsoes).allSatisfy(previsao -> {
            assertThat(previsao.getLocation()).isSameAs(SAO_PAULO);
            assertThat(previsao.getForecastAt().atZone(ZONA).toLocalDate())
                    .isEqualTo(LocalDate.parse("2026-10-30"));
            assertThat(previsao.getTemperature()).isBetween(-90.0, 60.0);
        });
        assertThat(previsoes.get(0).getForecastAt().atZone(ZONA).toLocalTime())
                .isEqualTo(java.time.LocalTime.MIDNIGHT);
    }

    @ParameterizedTest
    @EnumSource(Cenario.class)
    void simulaSeteDiasComMinimaMenorQueMaxima(Cenario cenario) throws Exception {
        ClienteSimulado cliente = novoCliente(cenario);

        List<PrevisaoDiaria> previsoes = cliente.buscarDiarias(SAO_PAULO);

        assertThat(previsoes).hasSize(7);
        assertThat(previsoes).extracting(PrevisaoDiaria::getForecastDate).isSorted()
                .doesNotHaveDuplicates();
        assertThat(previsoes.get(0).getForecastDate()).isEqualTo(LocalDate.parse("2026-10-30"));
        assertThat(previsoes.get(6).getForecastDate()).isEqualTo(LocalDate.parse("2026-11-05"));
        assertThat(previsoes).allSatisfy(previsao -> {
            assertThat(previsao.getMinTemperature()).isLessThan(previsao.getMaxTemperature());
            assertThat(previsao.getMinTemperature()).isBetween(-90.0, 60.0);
            assertThat(previsao.getMaxTemperature()).isBetween(-90.0, 60.0);
        });
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

    private static ClienteSimulado novoCliente(Cenario cenario) {
        return new ClienteSimulado(cenario.name(), FUSO, RELOGIO);
    }
}
