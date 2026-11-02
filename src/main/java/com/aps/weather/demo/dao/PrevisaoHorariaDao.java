package com.aps.weather.demo.dao;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoHoraria;

import java.time.Instant;
import java.util.List;

/** Acesso ao snapshot de previsões horárias de uma localização. */
public interface PrevisaoHorariaDao {

    List<PrevisaoHoraria> snapshotDe(Localizacao localizacao, Instant inicio, Instant fim);

    /**
     * Substitui o snapshot inteiro da localização em uma única transação: apaga o
     * anterior, descarta horários repetidos e grava o novo conjunto.
     */
    void substituirSnapshot(Localizacao localizacao, List<PrevisaoHoraria> previsoes);
}
