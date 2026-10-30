package com.aps.weather.demo.repository;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PrevisaoRepository extends JpaRepository<PrevisaoDoCLima, Long> {

    @EntityGraph(attributePaths = "location")
    Optional<PrevisaoDoCLima> findFirstByLocationOrderByForecastAtDesc(Localizacao location);

    @EntityGraph(attributePaths = "location")
    Page<PrevisaoDoCLima> findByLocation(Localizacao location, Pageable pageable);

    boolean existsByLocationAndForecastAt(Localizacao location, Instant forecastAt);
}
