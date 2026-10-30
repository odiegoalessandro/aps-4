package com.aps.weather.demo;

import com.aps.weather.demo.repository.LocalizacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os demais testes usam {@code ddl-auto=none}, então nunca exercitam a validação de
 * schema que roda em produção. Este contexto sobe com {@code validate} e falha no
 * startup se as entidades e as migrações divergirem — como acontecia com o
 * {@code id INTEGER} exigido pelo rowid contra um {@code Long} mapeado como BIGINT.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class ValidacaoSchemaTests {

    @Autowired
    private LocalizacaoRepository localizacaoRepository;

    @Test
    void contextoIniciaComEntidadesCompativeisComAsMigracoes() {
        assertThat(localizacaoRepository.count()).isGreaterThanOrEqualTo(5);
    }
}
