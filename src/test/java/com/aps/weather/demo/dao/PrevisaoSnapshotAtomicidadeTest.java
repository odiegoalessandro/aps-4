package com.aps.weather.demo.dao;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import com.aps.weather.demo.repository.LocalizacaoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A troca do snapshot só é segura se apagar e gravar acontecerem na mesma
 * transação. Estes testes não usam {@code @Transactional} justamente para que a
 * transação do DAO seja a de verdade: a falha precisa desfazer o delete já
 * executado. Os dados criados são removidos no {@code @AfterEach}.
 */
@SpringBootTest
class PrevisaoSnapshotAtomicidadeTest {

    private static final Instant INICIO = Instant.parse("2026-11-02T00:00:00Z");
    private static final LocalDate HOJE = LocalDate.parse("2026-11-02");
    private static final Instant ATUALIZADO_EM = Instant.parse("2026-11-02T12:00:00Z");

    @Autowired
    private PrevisaoHorariaDao horariaDao;

    @Autowired
    private PrevisaoDiariaDao diariaDao;

    @Autowired
    private LocalizacaoDao localizacaoDao;

    @Autowired
    private LocalizacaoRepository localizacaoRepository;

    private Localizacao local;

    @BeforeEach
    void prepararCidade() {
        local = localizacaoDao.salvarEFlushar(
                new Localizacao("Cidade Dao Atomica", -12.0, -22.0, "UTC"));
    }

    @AfterEach
    void limpar() {
        horariaDao.substituirSnapshot(local, List.of());
        diariaDao.substituirSnapshot(local, List.of());
        localizacaoRepository.delete(local);
    }

    @Test
    void falhaAoGravarHorariasPreservaOSnapshotAnterior() {
        horariaDao.substituirSnapshot(local, List.of(
                new PrevisaoHoraria(local, INICIO, 20.0, ATUALIZADO_EM),
                new PrevisaoHoraria(local, INICIO.plus(1, ChronoUnit.HOURS), 21.0, ATUALIZADO_EM)));

        PrevisaoHoraria semLocalizacao = new PrevisaoHoraria(
                null, INICIO.plus(2, ChronoUnit.HOURS), 22.0, ATUALIZADO_EM);

        assertThatThrownBy(() -> horariaDao.substituirSnapshot(local, List.of(semLocalizacao)));

        assertThat(horariaDao.snapshotDe(local, INICIO, INICIO.plus(3, ChronoUnit.HOURS)))
                .extracting(PrevisaoHoraria::getForecastAt)
                .containsExactly(INICIO, INICIO.plus(1, ChronoUnit.HOURS));
    }

    @Test
    void falhaAoGravarDiariasPreservaOSnapshotAnterior() {
        diariaDao.substituirSnapshot(local, List.of(
                new PrevisaoDiaria(local, HOJE, 18.0, 27.0, ATUALIZADO_EM),
                new PrevisaoDiaria(local, HOJE.plusDays(1), 19.0, 28.0, ATUALIZADO_EM)));

        PrevisaoDiaria semLocalizacao = new PrevisaoDiaria(
                null, HOJE.plusDays(2), 20.0, 29.0, ATUALIZADO_EM);

        assertThatThrownBy(() -> diariaDao.substituirSnapshot(local, List.of(semLocalizacao)));

        assertThat(diariaDao.snapshotDe(local, HOJE, HOJE.plusDays(2)))
                .extracting(PrevisaoDiaria::getForecastDate)
                .containsExactly(HOJE, HOJE.plusDays(1));
    }
}
