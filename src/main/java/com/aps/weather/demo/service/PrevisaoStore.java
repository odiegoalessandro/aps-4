package com.aps.weather.demo.service;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import com.aps.weather.demo.repository.PrevisaoDiariaRepository;
import com.aps.weather.demo.repository.PrevisaoHorariaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acesso às previsões persistidas. Cada coleta substitui o snapshot inteiro da
 * localização: guardar histórico não faz parte do requisito e manter linhas antigas
 * complicaria a regra de atualidade.
 */
@Component
public class PrevisaoStore {

    private final PrevisaoHorariaRepository horariaRepository;
    private final PrevisaoDiariaRepository diariaRepository;

    public PrevisaoStore(PrevisaoHorariaRepository horariaRepository,
                         PrevisaoDiariaRepository diariaRepository) {
        this.horariaRepository = horariaRepository;
        this.diariaRepository = diariaRepository;
    }

    @Transactional(readOnly = true)
    public List<PrevisaoHoraria> horarias(Localizacao local, Instant inicio, Instant fim) {
        return horariaRepository
                .findByLocationAndForecastAtGreaterThanEqualAndForecastAtLessThanOrderByForecastAtAsc(
                        local, inicio, fim);
    }

    @Transactional(readOnly = true)
    public List<PrevisaoDiaria> diarias(Localizacao local, LocalDate inicio, LocalDate fim) {
        return diariaRepository
                .findByLocationAndForecastDateBetweenOrderByForecastDateAsc(local, inicio, fim);
    }

    @Transactional
    public void substituirHorarias(Localizacao local, List<PrevisaoHoraria> previsoes) {
        horariaRepository.apagarPorLocalizacao(local);
        Map<Instant, PrevisaoHoraria> porHorario = new LinkedHashMap<>();
        for (PrevisaoHoraria previsao : previsoes) {
            porHorario.put(previsao.getForecastAt(), previsao);
        }
        horariaRepository.saveAll(porHorario.values());
    }

    @Transactional
    public void substituirDiarias(Localizacao local, List<PrevisaoDiaria> previsoes) {
        diariaRepository.apagarPorLocalizacao(local);
        Map<LocalDate, PrevisaoDiaria> porData = new LinkedHashMap<>();
        for (PrevisaoDiaria previsao : previsoes) {
            porData.put(previsao.getForecastDate(), previsao);
        }
        diariaRepository.saveAll(porData.values());
    }
}
