package com.aps.weather.demo.dao;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PrevisaoDiariaDaoTest {

    private static final LocalDate HOJE = LocalDate.parse("2026-11-02");
    private static final Instant ATUALIZADO_EM = Instant.parse("2026-11-02T12:00:00Z");

    @Autowired
    private PrevisaoDiariaDao dao;

    @Autowired
    private LocalizacaoDao localizacaoDao;

    private Localizacao local;

    @BeforeEach
    void prepararCidade() {
        local = localizacaoDao.salvarEFlushar(
                new Localizacao("Cidade Dao Diaria", -10.0, -20.0, "UTC"));
    }

    /**
     * A inserção fora de ordem garante que o resultado não dependa da ordem de
     * gravação. No SQLite o índice ux_daily_location_forecast_date já devolve o
     * intervalo em ordem crescente, então remover o ORDER BY não muda o resultado
     * observável; a ordenação decrescente, essa sim, é detectada por esta asserção.
     */
    @Test
    void snapshotIncluiAsDuasPontasDoIntervaloEmOrdemCrescente() {
        dao.substituirSnapshot(local, dias(3, 0, 4, 1, 2));

        List<PrevisaoDiaria> snapshot = dao.snapshotDe(local, HOJE.plusDays(1), HOJE.plusDays(3));

        assertThat(snapshot)
                .extracting(PrevisaoDiaria::getForecastDate)
                .containsExactly(HOJE.plusDays(1), HOJE.plusDays(2), HOJE.plusDays(3));
    }

    @Test
    void substituirSnapshotNaoApagaOsDadosDeOutraLocalizacao() {
        Localizacao outra = localizacaoDao.salvarEFlushar(
                new Localizacao("Cidade Dao Diaria Outra", -11.0, -21.0, "UTC"));
        dao.substituirSnapshot(local, dias(0, 1));
        dao.substituirSnapshot(outra, List.of(
                new PrevisaoDiaria(outra, HOJE.plusDays(10), 15.0, 25.0, ATUALIZADO_EM),
                new PrevisaoDiaria(outra, HOJE.plusDays(11), 16.0, 26.0, ATUALIZADO_EM)));

        dao.substituirSnapshot(local, dias(2, 3));

        assertThat(dao.snapshotDe(local, HOJE, HOJE.plusDays(4)))
                .extracting(PrevisaoDiaria::getForecastDate)
                .containsExactly(HOJE.plusDays(2), HOJE.plusDays(3));
        assertThat(dao.snapshotDe(outra, HOJE, HOJE.plusDays(12)))
                .extracting(PrevisaoDiaria::getForecastDate)
                .containsExactly(HOJE.plusDays(10), HOJE.plusDays(11));
    }

    @Test
    void substituirSnapshotApagaOConteudoAnterior() {
        dao.substituirSnapshot(local, dias(0, 1));

        dao.substituirSnapshot(local, dias(7, 8));

        assertThat(dao.snapshotDe(local, HOJE, HOJE.plusDays(1))).isEmpty();
        assertThat(dao.snapshotDe(local, HOJE.plusDays(7), HOJE.plusDays(8))).hasSize(2);
    }

    @Test
    void substituirSnapshotDeduplicaAMesmaData() {
        PrevisaoDiaria primeira = diaria(0, 18.0, 27.0);
        PrevisaoDiaria duplicada = diaria(0, 19.0, 30.0);

        dao.substituirSnapshot(local, List.of(primeira, duplicada));

        List<PrevisaoDiaria> snapshot = dao.snapshotDe(local, HOJE, HOJE);
        assertThat(snapshot).hasSize(1);
        assertThat(snapshot.get(0).getMaxTemperature()).isEqualTo(30.0);
    }

    @Test
    void substituirSnapshotComListaVaziaLimpaOsDados() {
        dao.substituirSnapshot(local, dias(0, 1));

        dao.substituirSnapshot(local, List.of());

        assertThat(dao.snapshotDe(local, HOJE, HOJE.plusDays(1))).isEmpty();
    }

    private List<PrevisaoDiaria> dias(int... dias) {
        return IntStream.of(dias)
                .mapToObj(dia -> diaria(dia, 18.0 + dia, 27.0 + dia))
                .toList();
    }

    private PrevisaoDiaria diaria(int dia, double minima, double maxima) {
        return new PrevisaoDiaria(local, HOJE.plusDays(dia), minima, maxima, ATUALIZADO_EM);
    }
}
