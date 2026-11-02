package com.aps.weather.demo.repository;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PrevisaoDiariaRepository extends JpaRepository<PrevisaoDiaria, Long> {

    List<PrevisaoDiaria> findByLocationAndForecastDateBetweenOrderByForecastDateAsc(
            Localizacao location, LocalDate inicio, LocalDate fim);

    /**
     * O clear/flush automático evita que entidades ainda gerenciadas colidam com os
     * ids reutilizados pelo SQLite quando o snapshot é substituído mais de uma vez na
     * mesma transação. Efeito colateral: o contexto de persistência é limpo, então o
     * chamador não deve reutilizar entidades carregadas antes desta chamada.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PrevisaoDiaria p where p.location = :location")
    void apagarPorLocalizacao(@Param("location") Localizacao location);
}
