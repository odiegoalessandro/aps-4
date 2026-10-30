package com.aps.weather.demo.repository;

import com.aps.weather.demo.domain.Localizacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocalizacaoRepository extends JpaRepository<Localizacao, Long> {

    Optional<Localizacao> findByCityNormalizada(String cityNormalizada);

    List<Localizacao> findAllByOrderByCityAsc();
}
