package com.aps.weather.demo.repository;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PrevisaoHorariaRepository extends JpaRepository<PrevisaoHoraria, Long> {

    List<PrevisaoHoraria> findByLocationAndForecastAtGreaterThanEqualAndForecastAtLessThanOrderByForecastAtAsc(
            Localizacao location, Instant inicio, Instant fim);

    /**
     * O clear/flush automático evita que entidades ainda gerenciadas colidam com os
     * ids reutilizados pelo SQLite quando o snapshot é substituído mais de uma vez na
     * mesma transação. Efeito colateral: o contexto de persistência é limpo, então o
     * chamador não deve reutilizar entidades carregadas antes desta chamada.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PrevisaoHoraria p where p.location = :location")
    void apagarPorLocalizacao(@Param("location") Localizacao location);
}
