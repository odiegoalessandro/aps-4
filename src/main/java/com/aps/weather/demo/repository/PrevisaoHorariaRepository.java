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

    @Modifying
    @Query("delete from PrevisaoHoraria p where p.location = :location")
    void apagarPorLocalizacao(@Param("location") Localizacao location);
}
