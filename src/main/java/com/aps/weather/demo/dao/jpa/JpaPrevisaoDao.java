package com.aps.weather.demo.dao.jpa;

import com.aps.weather.demo.dao.PrevisaoDao;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.repository.PrevisaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public class JpaPrevisaoDao implements PrevisaoDao {

    private final PrevisaoRepository repository;

    public JpaPrevisaoDao(PrevisaoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrevisaoDoCLima> ultimaDe(Localizacao localizacao) {
        return repository.findFirstByLocationOrderByForecastAtDesc(localizacao);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrevisaoDoCLima> historicoDe(Localizacao localizacao, Pageable pageable) {
        return repository.findByLocation(localizacao, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePara(Localizacao localizacao, Instant forecastAt) {
        return repository.existsByLocationAndForecastAt(localizacao, forecastAt);
    }

    @Override
    @Transactional
    public PrevisaoDoCLima salvar(PrevisaoDoCLima previsao) {
        return repository.save(previsao);
    }
}
