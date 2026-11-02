package com.aps.weather.demo.service;

import com.aps.weather.demo.dao.PrevisaoDiariaDao;
import com.aps.weather.demo.dao.PrevisaoHorariaDao;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Acesso às previsões persistidas. Cada coleta substitui o snapshot inteiro da
 * localização: guardar histórico não faz parte do requisito e manter linhas antigas
 * complicaria a regra de atualidade.
 */
@Component
public class PrevisaoStore {

    private final PrevisaoHorariaDao horariaDao;
    private final PrevisaoDiariaDao diariaDao;

    public PrevisaoStore(PrevisaoHorariaDao horariaDao, PrevisaoDiariaDao diariaDao) {
        this.horariaDao = horariaDao;
        this.diariaDao = diariaDao;
    }

    public List<PrevisaoHoraria> horarias(Localizacao local, Instant inicio, Instant fim) {
        return horariaDao.snapshotDe(local, inicio, fim);
    }

    public List<PrevisaoDiaria> diarias(Localizacao local, LocalDate inicio, LocalDate fim) {
        return diariaDao.snapshotDe(local, inicio, fim);
    }

    public void substituirHorarias(Localizacao local, List<PrevisaoHoraria> previsoes) {
        horariaDao.substituirSnapshot(local, previsoes);
    }

    public void substituirDiarias(Localizacao local, List<PrevisaoDiaria> previsoes) {
        diariaDao.substituirSnapshot(local, previsoes);
    }
}
