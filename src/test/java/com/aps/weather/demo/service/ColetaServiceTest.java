package com.aps.weather.demo.service;

import com.aps.weather.demo.client.FonteDeClima;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.repository.LocalizacaoRepository;
import com.aps.weather.demo.repository.PrevisaoRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColetaServiceTest {

    private static final Instant QUANDO = Instant.parse("2026-10-30T15:00:00Z");

    @Mock
    private FonteDeClima fonte;
    @Mock
    private LocalizacaoRepository localizacoes;
    @Mock
    private PrevisaoRepository previsoes;

    @Test
    void corridaDeCriacaoUsaACidadeQueVenceuNoBanco() throws Exception {
        Localizacao desejada = new Localizacao("São Paulo", -23.55, -46.63, "America/Sao_Paulo");
        Localizacao existente = new Localizacao("São Paulo", -23.55, -46.63, "America/Sao_Paulo");
        ColetaService servico = new ColetaService(fonte, localizacoes, previsoes, true, new SimpleMeterRegistry());

        when(localizacoes.findByCityNormalizada("sao paulo"))
                .thenReturn(Optional.empty(), Optional.of(existente));
        when(localizacoes.saveAndFlush(desejada))
                .thenThrow(new DataIntegrityViolationException("ux_location_city_normalizada"));
        when(fonte.buscar(existente)).thenAnswer(chamada -> previsao(chamada.getArgument(0)));
        when(previsoes.existsByLocationAndForecastAt(any(), any())).thenReturn(false);

        List<PrevisaoDoCLima> gravadas = servico.coletarCidade(desejada);

        assertThat(gravadas).hasSize(1);
        assertThat(gravadas.get(0).getLocation()).isSameAs(existente);
    }

    @Test
    void violacaoSemCidadeGravadaEhPropagada() {
        Localizacao desejada = new Localizacao("Sem Par", 0.0, 0.0, "UTC");
        DataIntegrityViolationException erro = new DataIntegrityViolationException("falhou");
        ColetaService servico = new ColetaService(fonte, localizacoes, previsoes, true, new SimpleMeterRegistry());

        when(localizacoes.findByCityNormalizada("sem par")).thenReturn(Optional.empty());
        when(localizacoes.saveAndFlush(desejada)).thenThrow(erro);

        assertThatThrownBy(() -> servico.coletarCidade(desejada)).isSameAs(erro);
    }

    @Test
    void corridaNaGravacaoDaPrevisaoEhTratadaComoSucesso() throws Exception {
        Localizacao local = new Localizacao("São Paulo", -23.55, -46.63, "America/Sao_Paulo");
        ColetaService servico = new ColetaService(fonte, localizacoes, previsoes, true, new SimpleMeterRegistry());

        when(localizacoes.findByCityNormalizada("sao paulo")).thenReturn(Optional.of(local));
        when(fonte.buscar(local)).thenAnswer(chamada -> previsao(chamada.getArgument(0)));
        when(previsoes.existsByLocationAndForecastAt(any(), any())).thenReturn(false);
        when(previsoes.save(any())).thenThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException("UNIQUE constraint failed: weather_forecasts.location_id,"
                        + " weather_forecasts.forecast_at")));

        assertThat(servico.coletarCidade(local)).isEmpty();
    }

    @Test
    void violacaoDeOutraConstraintEhPropagada() throws Exception {
        Localizacao local = new Localizacao("Sem Par", 0.0, 0.0, "UTC");
        DataIntegrityViolationException erro = new DataIntegrityViolationException(
                "falhou",
                new SQLException("UNIQUE constraint failed: locations.city_normalizada"));
        ColetaService servico = new ColetaService(fonte, localizacoes, previsoes, true, new SimpleMeterRegistry());

        when(localizacoes.findByCityNormalizada("sem par")).thenReturn(Optional.of(local));
        when(fonte.buscar(local)).thenAnswer(chamada -> previsao(chamada.getArgument(0)));
        when(previsoes.existsByLocationAndForecastAt(any(), any())).thenReturn(false);
        when(previsoes.save(any())).thenThrow(erro);

        assertThatThrownBy(() -> servico.coletarCidade(local)).isSameAs(erro);
    }

    private static PrevisaoDoCLima previsao(Localizacao local) {
        return new PrevisaoDoCLima(local, QUANDO, 20, 20, 50, 0, 3, 5, 90, 1010, 10, QUANDO);
    }
}
