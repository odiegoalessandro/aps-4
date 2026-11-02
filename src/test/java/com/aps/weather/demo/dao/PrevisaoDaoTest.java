package com.aps.weather.demo.dao;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PrevisaoDaoTest {

    private static final Instant BASE = Instant.parse("2026-10-30T12:00:00Z");

    @Autowired
    private PrevisaoDao previsaoDao;

    @Autowired
    private LocalizacaoDao localizacaoDao;

    private Localizacao local;

    @BeforeEach
    void prepararCidade() {
        local = localizacaoDao.salvarEFlushar(
                new Localizacao("Cidade Dao Previsao", -10.0, -20.0, "UTC"));
    }

    @Test
    void ultimaDeDevolveAPrevisaoMaisRecente() {
        previsaoDao.salvar(previsao(BASE));
        previsaoDao.salvar(previsao(BASE.plusSeconds(3600)));
        previsaoDao.salvar(previsao(BASE.plusSeconds(1800)));

        assertThat(previsaoDao.ultimaDe(local))
                .get()
                .extracting(PrevisaoDoCLima::getForecastAt)
                .isEqualTo(BASE.plusSeconds(3600));
    }

    @Test
    void ultimaDeSemPrevisoesEhVazio() {
        assertThat(previsaoDao.ultimaDe(local)).isEmpty();
    }

    @Test
    void historicoDeRespeitaPaginacaoEOrdenacao() {
        previsaoDao.salvar(previsao(BASE));
        previsaoDao.salvar(previsao(BASE.plusSeconds(1800)));
        previsaoDao.salvar(previsao(BASE.plusSeconds(3600)));

        Page<PrevisaoDoCLima> pagina = previsaoDao.historicoDe(local,
                PageRequest.of(0, 2, Sort.by(
                        Sort.Order.desc("forecastAt"), Sort.Order.desc("id"))));

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getContent())
                .extracting(PrevisaoDoCLima::getForecastAt)
                .containsExactly(BASE.plusSeconds(3600), BASE.plusSeconds(1800));
    }

    @Test
    void existeParaDistingueHorarioGravadoDeHorarioNovo() {
        previsaoDao.salvar(previsao(BASE));

        assertThat(previsaoDao.existePara(local, BASE)).isTrue();
        assertThat(previsaoDao.existePara(local, BASE.plusSeconds(3600))).isFalse();
    }

    private PrevisaoDoCLima previsao(Instant quando) {
        return new PrevisaoDoCLima(local, quando, 20.0, 19.0, 60, 0.0, 0,
                10.0, 90.0, 1010.0, 20.0, quando);
    }
}
