package com.aps.weather.demo.dao.jpa;

import com.aps.weather.demo.dao.PrevisaoDiariaDao;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import com.aps.weather.demo.repository.PrevisaoDiariaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JpaPrevisaoDiariaDao implements PrevisaoDiariaDao {

    private final PrevisaoDiariaRepository repository;

    public JpaPrevisaoDiariaDao(PrevisaoDiariaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrevisaoDiaria> snapshotDe(Localizacao localizacao, LocalDate inicio, LocalDate fim) {
        return repository.findByLocationAndForecastDateBetweenOrderByForecastDateAsc(
                localizacao, inicio, fim);
    }

    @Override
    @Transactional
    public void substituirSnapshot(Localizacao localizacao, List<PrevisaoDiaria> previsoes) {
        repository.apagarPorLocalizacao(localizacao);
        Map<LocalDate, PrevisaoDiaria> porData = new LinkedHashMap<>();
        for (PrevisaoDiaria previsao : previsoes) {
            porData.put(previsao.getForecastDate(), previsao);
        }
        repository.saveAll(porData.values());
    }
}
