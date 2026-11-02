package com.aps.weather.demo.dao;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PrevisaoHorariaDaoTest {

    private static final Instant INICIO = Instant.parse("2026-11-02T00:00:00Z");

    @Autowired
    private PrevisaoHorariaDao dao;

    @Autowired
    private LocalizacaoDao localizacaoDao;

    private Localizacao local;

    @BeforeEach
    void prepararCidade() {
        local = localizacaoDao.salvarEFlushar(
                new Localizacao("Cidade Dao Horaria", -10.0, -20.0, "UTC"));
    }

    /**
     * A inserção fora de ordem garante que o resultado não dependa da ordem de
     * gravação. No SQLite o índice ux_hourly_location_forecast_at já devolve o
     * intervalo em ordem crescente, então remover o ORDER BY não muda o resultado
     * observável; a ordenação decrescente, essa sim, é detectada por esta asserção.
     */
    @Test
    void snapshotUsaIntervaloFechadoNoInicioEAbertoNoFimEmOrdemCrescente() {
        dao.substituirSnapshot(local, horas(2, 0, 3, 1));

        List<PrevisaoHoraria> snapshot = dao.snapshotDe(local,
                INICIO.plus(1, ChronoUnit.HOURS), INICIO.plus(3, ChronoUnit.HOURS));

        assertThat(snapshot)
                .extracting(PrevisaoHoraria::getForecastAt)
                .containsExactly(INICIO.plus(1, ChronoUnit.HOURS), INICIO.plus(2, ChronoUnit.HOURS));
    }

    @Test
    void substituirSnapshotNaoApagaOsDadosDeOutraLocalizacao() {
        Localizacao outra = localizacaoDao.salvarEFlushar(
                new Localizacao("Cidade Dao Horaria Outra", -11.0, -21.0, "UTC"));
        dao.substituirSnapshot(local, horas(0, 1));
        dao.substituirSnapshot(outra, List.of(
                new PrevisaoHoraria(outra, INICIO.plus(10, ChronoUnit.HOURS), 21.0, INICIO),
                new PrevisaoHoraria(outra, INICIO.plus(11, ChronoUnit.HOURS), 22.0, INICIO)));

        dao.substituirSnapshot(local, horas(2, 3));

        assertThat(dao.snapshotDe(local, INICIO, INICIO.plus(4, ChronoUnit.HOURS)))
                .extracting(PrevisaoHoraria::getForecastAt)
                .containsExactly(INICIO.plus(2, ChronoUnit.HOURS), INICIO.plus(3, ChronoUnit.HOURS));
        assertThat(dao.snapshotDe(outra, INICIO, INICIO.plus(12, ChronoUnit.HOURS)))
                .extracting(PrevisaoHoraria::getForecastAt)
                .containsExactly(INICIO.plus(10, ChronoUnit.HOURS), INICIO.plus(11, ChronoUnit.HOURS));
    }

    @Test
    void substituirSnapshotApagaOConteudoAnterior() {
        dao.substituirSnapshot(local, horas(0, 1));

        dao.substituirSnapshot(local, horas(24, 25));

        assertThat(dao.snapshotDe(local, INICIO, INICIO.plus(2, ChronoUnit.HOURS))).isEmpty();
        assertThat(dao.snapshotDe(local, INICIO.plus(24, ChronoUnit.HOURS),
                INICIO.plus(26, ChronoUnit.HOURS))).hasSize(2);
    }

    @Test
    void substituirSnapshotDeduplicaOMesmoHorario() {
        PrevisaoHoraria primeira = horaria(0, 20.0);
        PrevisaoHoraria duplicada = horaria(0, 25.0);

        dao.substituirSnapshot(local, List.of(primeira, duplicada));

        List<PrevisaoHoraria> snapshot = dao.snapshotDe(local, INICIO,
                INICIO.plus(1, ChronoUnit.HOURS));
        assertThat(snapshot).hasSize(1);
        assertThat(snapshot.get(0).getTemperature()).isEqualTo(25.0);
    }

    @Test
    void substituirSnapshotComListaVaziaLimpaOsDados() {
        dao.substituirSnapshot(local, horas(0, 1));

        dao.substituirSnapshot(local, List.of());

        assertThat(dao.snapshotDe(local, INICIO, INICIO.plus(2, ChronoUnit.HOURS))).isEmpty();
    }

    private List<PrevisaoHoraria> horas(int... horas) {
        return IntStream.of(horas)
                .mapToObj(hora -> horaria(hora, 20.0 + hora))
                .toList();
    }

    private PrevisaoHoraria horaria(int hora, double temperatura) {
        return new PrevisaoHoraria(local, INICIO.plus(hora, ChronoUnit.HOURS), temperatura, INICIO);
    }
}
