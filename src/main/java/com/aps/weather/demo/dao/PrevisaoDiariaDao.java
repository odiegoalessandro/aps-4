package com.aps.weather.demo.dao;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;

import java.time.LocalDate;
import java.util.List;

/** Acesso ao snapshot de previsões diárias de uma localização. */
public interface PrevisaoDiariaDao {

    List<PrevisaoDiaria> snapshotDe(Localizacao localizacao, LocalDate inicio, LocalDate fim);

    /**
     * Substitui o snapshot inteiro da localização em uma única transação: apaga o
     * anterior, descarta datas repetidas e grava o novo conjunto.
     */
    void substituirSnapshot(Localizacao localizacao, List<PrevisaoDiaria> previsoes);
}
