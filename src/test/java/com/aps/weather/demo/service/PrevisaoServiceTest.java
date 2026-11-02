package com.aps.weather.demo.service;

import com.aps.weather.demo.client.FonteDePrevisao;
import com.aps.weather.demo.config.FusoHorario;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import com.aps.weather.demo.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrevisaoServiceTest {

    private static final long TTL_MS = 900_000;
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate HOJE = LocalDate.now(RELOGIO.withZone(ZONA));
    private static final Instant AGORA = RELOGIO.instant();
    private static final Instant RECENTE = AGORA.minusSeconds(60);
    private static final Instant VENCIDO = AGORA.minusMillis(TTL_MS + 1);

    private static final Localizacao SAO_PAULO =
            new Localizacao("São Paulo", -23.55, -46.63, "America/Sao_Paulo");

    @Mock
    private WeatherQueryService cidades;
    @Mock
    private FonteDePrevisao fonte;
    @Mock
    private PrevisaoStore store;

    private PrevisaoService servico;

    @BeforeEach
    void criarServico() {
        servico = new PrevisaoService(cidades, fonte, store,
                new FusoHorario("America/Sao_Paulo"), RELOGIO, TTL_MS);
        lenient().when(cidades.buscarCidade("São Paulo")).thenReturn(SAO_PAULO);
    }

    @Test
    void snapshotHorarioRecenteEhServidoSemConsultarAFonte() throws Exception {
        List<PrevisaoHoraria> snapshot = horarias(RECENTE);
        when(store.horarias(eq(SAO_PAULO), any(), any())).thenReturn(snapshot);

        ResultadoPrevisaoHoraria resultado = servico.horarias("São Paulo");

        assertThat(resultado.data()).isEqualTo(HOJE);
        assertThat(resultado.fuso()).isEqualTo(ZONA);
        assertThat(resultado.previsoes()).isSameAs(snapshot);
        verifyNoInteractions(fonte);
    }

    @Test
    void snapshotHorarioVencidoEhSubstituidoPelaFonte() throws Exception {
        List<PrevisaoHoraria> vencido = horarias(VENCIDO);
        List<PrevisaoHoraria> novos = horarias(AGORA);
        when(store.horarias(eq(SAO_PAULO), any(), any()))
                .thenReturn(vencido, vencido, novos);
        when(fonte.buscarHorarias(SAO_PAULO)).thenReturn(novos);

        ResultadoPrevisaoHoraria resultado = servico.horarias("São Paulo");

        assertThat(resultado.previsoes()).isSameAs(novos);
        verify(store).substituirHorarias(SAO_PAULO, novos);
    }

    @Test
    void falhaDaFonteServeSnapshotHorarioVencido() throws Exception {
        List<PrevisaoHoraria> vencido = horarias(VENCIDO);
        when(store.horarias(eq(SAO_PAULO), any(), any())).thenReturn(vencido, vencido);
        when(fonte.buscarHorarias(SAO_PAULO)).thenThrow(new ApiException(500, "erro"));

        ResultadoPrevisaoHoraria resultado = servico.horarias("São Paulo");

        assertThat(resultado.previsoes()).isSameAs(vencido);
        verify(store, never()).substituirHorarias(any(), any());
    }

    @Test
    void snapshotHorarioIncompletoEhSubstituidoMesmoDentroDoTtl() throws Exception {
        List<PrevisaoHoraria> incompleto = horarias(RECENTE).subList(0, 23);
        List<PrevisaoHoraria> novos = horarias(AGORA);
        when(store.horarias(eq(SAO_PAULO), any(), any()))
                .thenReturn(incompleto, incompleto, novos);
        when(fonte.buscarHorarias(SAO_PAULO)).thenReturn(novos);

        ResultadoPrevisaoHoraria resultado = servico.horarias("São Paulo");

        assertThat(resultado.previsoes()).isSameAs(novos);
        verify(store).substituirHorarias(SAO_PAULO, novos);
    }

    @Test
    void falhaDaFonteComSnapshotHorarioIncompletoEhPropagada() throws Exception {
        List<PrevisaoHoraria> incompleto = horarias(RECENTE).subList(0, 23);
        when(store.horarias(eq(SAO_PAULO), any(), any())).thenReturn(incompleto, incompleto);
        when(fonte.buscarHorarias(SAO_PAULO)).thenThrow(new ApiException(500, "erro"));

        assertThatThrownBy(() -> servico.horarias("São Paulo"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void falhaDaFonteSemSnapshotHorarioEhPropagada() throws Exception {
        when(store.horarias(eq(SAO_PAULO), any(), any())).thenReturn(List.of(), List.of());
        when(fonte.buscarHorarias(SAO_PAULO)).thenThrow(new ApiException(500, "erro"));

        assertThatThrownBy(() -> servico.horarias("São Paulo"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void snapshotDiarioRecenteECCompletoEhServido() throws Exception {
        List<PrevisaoDiaria> snapshot = diarias(RECENTE);
        when(store.diarias(SAO_PAULO, HOJE, HOJE.plusDays(6))).thenReturn(snapshot);

        ResultadoPrevisaoDiaria resultado = servico.diarias("São Paulo");

        assertThat(resultado.fuso()).isEqualTo(ZONA);
        assertThat(resultado.previsoes()).isSameAs(snapshot);
        verifyNoInteractions(fonte);
    }

    @Test
    void snapshotDiarioIncompletoEhSubstituidoMesmoDentroDoTtl() throws Exception {
        List<PrevisaoDiaria> incompleto = diarias(RECENTE).subList(0, 6);
        List<PrevisaoDiaria> novos = diarias(AGORA);
        when(store.diarias(SAO_PAULO, HOJE, HOJE.plusDays(6)))
                .thenReturn(incompleto, incompleto, novos);
        when(fonte.buscarDiarias(SAO_PAULO)).thenReturn(novos);

        ResultadoPrevisaoDiaria resultado = servico.diarias("São Paulo");

        assertThat(resultado.previsoes()).isSameAs(novos);
        verify(store).substituirDiarias(SAO_PAULO, novos);
    }

    @Test
    void snapshotDiarioComLacunaEhSubstituidoMesmoDentroDoTtl() throws Exception {
        List<PrevisaoDiaria> comLacuna = new ArrayList<>(diarias(RECENTE));
        comLacuna.set(3, new PrevisaoDiaria(SAO_PAULO, HOJE.plusDays(9), 20.0, 30.0, RECENTE));
        List<PrevisaoDiaria> novos = diarias(AGORA);
        when(store.diarias(SAO_PAULO, HOJE, HOJE.plusDays(6)))
                .thenReturn(comLacuna, comLacuna, novos);
        when(fonte.buscarDiarias(SAO_PAULO)).thenReturn(novos);

        ResultadoPrevisaoDiaria resultado = servico.diarias("São Paulo");

        assertThat(resultado.previsoes()).isSameAs(novos);
        verify(store).substituirDiarias(SAO_PAULO, novos);
    }

    @Test
    void falhaDaFonteComSnapshotDiarioIncompletoEhPropagada() throws Exception {
        List<PrevisaoDiaria> incompleto = diarias(RECENTE).subList(0, 6);
        when(store.diarias(SAO_PAULO, HOJE, HOJE.plusDays(6)))
                .thenReturn(incompleto, incompleto);
        when(fonte.buscarDiarias(SAO_PAULO)).thenThrow(new ApiException(500, "erro"));

        assertThatThrownBy(() -> servico.diarias("São Paulo"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void falhaDaFonteServeSnapshotDiarioVencido() throws Exception {
        List<PrevisaoDiaria> vencido = diarias(VENCIDO);
        when(store.diarias(SAO_PAULO, HOJE, HOJE.plusDays(6))).thenReturn(vencido, vencido);
        when(fonte.buscarDiarias(SAO_PAULO)).thenThrow(new ApiException(500, "erro"));

        ResultadoPrevisaoDiaria resultado = servico.diarias("São Paulo");

        assertThat(resultado.previsoes()).isSameAs(vencido);
        verify(store, never()).substituirDiarias(any(), any());
    }

    @Test
    void cidadeDesconhecidaNaoChegaAFonte() {
        when(cidades.buscarCidade("Atlântida"))
                .thenThrow(new com.aps.weather.demo.exception.CidadeNaoEncontradaException("Atlântida"));

        assertThatThrownBy(() -> servico.horarias("Atlântida"))
                .isInstanceOf(com.aps.weather.demo.exception.CidadeNaoEncontradaException.class);
        verifyNoInteractions(fonte, store);
    }

    private static List<PrevisaoHoraria> horarias(Instant atualizadoEm) {
        Instant inicio = HOJE.atStartOfDay(ZONA).toInstant();
        return IntStream.range(0, 24)
                .mapToObj(hora -> new PrevisaoHoraria(
                        SAO_PAULO, inicio.plus(hora, ChronoUnit.HOURS), 20.0 + hora, atualizadoEm))
                .toList();
    }

    private static List<PrevisaoDiaria> diarias(Instant atualizadoEm) {
        return IntStream.range(0, 7)
                .mapToObj(dia -> new PrevisaoDiaria(
                        SAO_PAULO, HOJE.plusDays(dia), 18.0 + dia, 27.0 + dia, atualizadoEm))
                .toList();
    }
}
