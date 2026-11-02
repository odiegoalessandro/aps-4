package com.aps.weather.demo.client;

import com.aps.weather.demo.exception.DadoInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorDePrevisaoTest {

    @Test
    void aceitaHorariasCompletas() {
        assertThatCode(() -> ValidadorDePrevisao.validarHoraria(
                horaria(horariosDoDia(), temperaturasDoDia())))
                .doesNotThrowAnyException();
    }

    @Test
    void recusaHorariasComMenosDe24Registros() {
        assertThatThrownBy(() -> ValidadorDePrevisao.validarHoraria(
                horaria(List.of("2026-11-02T00:00", "2026-11-02T01:00"), List.of(19.5, 19.2))))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("esperados 24");
    }

    @Test
    void recusaRespostaNula() {
        assertThatThrownBy(() -> ValidadorDePrevisao.validarHoraria(null))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("vazia");
    }

    @Test
    void recusaBlocoHorarioAusente() {
        PrevisaoHorariaResposta resposta = new PrevisaoHorariaResposta();

        assertThatThrownBy(() -> ValidadorDePrevisao.validarHoraria(resposta))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("'hourly'");
    }

    @Test
    void recusaArraysDeTamanhosDiferentes() {
        assertThatThrownBy(() -> ValidadorDePrevisao.validarHoraria(
                horaria(List.of("2026-11-02T00:00", "2026-11-02T01:00"), List.of(19.5))))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("difere");
    }

    @Test
    void recusaTemperaturaAusente() {
        assertThatThrownBy(() -> ValidadorDePrevisao.validarHoraria(
                horaria(List.of("2026-11-02T00:00"), java.util.Arrays.asList((Double) null))))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("ausente");
    }

    @Test
    void recusaTemperaturaForaDaFaixa() {
        assertThatThrownBy(() -> ValidadorDePrevisao.validarHoraria(
                horaria(List.of("2026-11-02T00:00"), List.of(120.0))))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("fora da faixa");
    }

    @Test
    void aceitaDiariasCompletas() {
        assertThatCode(() -> ValidadorDePrevisao.validarDiaria(
                diaria(datasDaSemana(), minimasDaSemana(), maximasDaSemana())))
                .doesNotThrowAnyException();
    }

    @Test
    void recusaDiariasComMenosDe7Registros() {
        assertThatThrownBy(() -> ValidadorDePrevisao.validarDiaria(
                diaria(List.of("2026-11-02"), List.of(18.0), List.of(27.0))))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("esperados 7");
    }

    @Test
    void recusaDiariaComMaximasDeTamanhoDiferente() {
        assertThatThrownBy(() -> ValidadorDePrevisao.validarDiaria(
                diaria(List.of("2026-11-02"), List.of(18.0), List.of())))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("máximas");
    }

    @Test
    void recusaMinimaMaiorQueMaxima() {
        assertThatThrownBy(() -> ValidadorDePrevisao.validarDiaria(
                diaria(List.of("2026-11-02"), List.of(30.0), List.of(20.0))))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("mínima maior");
    }

    private static List<String> horariosDoDia() {
        return IntStream.range(0, 24)
                .mapToObj(hora -> "2026-11-02T%02d:00".formatted(hora))
                .toList();
    }

    private static List<Double> temperaturasDoDia() {
        return IntStream.range(0, 24)
                .mapToObj(hora -> 19.5 + hora * 0.1)
                .toList();
    }

    private static List<String> datasDaSemana() {
        LocalDate primeiro = LocalDate.parse("2026-11-02");
        return IntStream.range(0, 7)
                .mapToObj(dia -> primeiro.plusDays(dia).toString())
                .toList();
    }

    private static List<Double> minimasDaSemana() {
        return IntStream.range(0, 7).mapToObj(dia -> 18.0 + dia).toList();
    }

    private static List<Double> maximasDaSemana() {
        return IntStream.range(0, 7).mapToObj(dia -> 27.0 + dia).toList();
    }

    private static PrevisaoHorariaResposta horaria(List<String> horarios, List<Double> temperaturas) {
        PrevisaoHorariaResposta resposta = new PrevisaoHorariaResposta();
        resposta.hourly = new PrevisaoHorariaResposta.Horaria();
        resposta.hourly.time = horarios;
        resposta.hourly.temperature2m = temperaturas;
        return resposta;
    }

    private static PrevisaoDiariaResposta diaria(List<String> datas, List<Double> minimas,
                                                 List<Double> maximas) {
        PrevisaoDiariaResposta resposta = new PrevisaoDiariaResposta();
        resposta.daily = new PrevisaoDiariaResposta.Diaria();
        resposta.daily.time = datas;
        resposta.daily.temperature2mMin = minimas;
        resposta.daily.temperature2mMax = maximas;
        return resposta;
    }
}
