package com.aps.weather.demo.dao;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

/** Acesso às previsões observadas/coletadas de uma localização. */
public interface PrevisaoDao {

    Optional<PrevisaoDoCLima> ultimaDe(Localizacao localizacao);

    Page<PrevisaoDoCLima> historicoDe(Localizacao localizacao, Pageable pageable);

    boolean existePara(Localizacao localizacao, Instant forecastAt);

    PrevisaoDoCLima salvar(PrevisaoDoCLima previsao);
}
