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

    @Modifying
    @Query("delete from PrevisaoDiaria p where p.location = :location")
    void apagarPorLocalizacao(@Param("location") Localizacao location);
}
