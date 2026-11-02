package com.aps.weather.demo.dao.jpa;

import com.aps.weather.demo.dao.PrevisaoHorariaDao;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import com.aps.weather.demo.repository.PrevisaoHorariaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JpaPrevisaoHorariaDao implements PrevisaoHorariaDao {

    private final PrevisaoHorariaRepository repository;

    public JpaPrevisaoHorariaDao(PrevisaoHorariaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrevisaoHoraria> snapshotDe(Localizacao localizacao, Instant inicio, Instant fim) {
        return repository
                .findByLocationAndForecastAtGreaterThanEqualAndForecastAtLessThanOrderByForecastAtAsc(
                        localizacao, inicio, fim);
    }

    @Override
    @Transactional
    public void substituirSnapshot(Localizacao localizacao, List<PrevisaoHoraria> previsoes) {
        repository.apagarPorLocalizacao(localizacao);
        Map<Instant, PrevisaoHoraria> porHorario = new LinkedHashMap<>();
        for (PrevisaoHoraria previsao : previsoes) {
            porHorario.put(previsao.getForecastAt(), previsao);
        }
        repository.saveAll(porHorario.values());
    }
}
