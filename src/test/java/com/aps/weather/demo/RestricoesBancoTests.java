package com.aps.weather.demo;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.repository.LocalizacaoRepository;
import com.aps.weather.demo.repository.PrevisaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica, pelo caminho do JPA, que as constraints criadas em V2 e V3 chegam ao banco.
 */
@SpringBootTest
class RestricoesBancoTests {

    @Autowired
    private LocalizacaoRepository localizacaoRepository;

    @Autowired
    private PrevisaoRepository previsaoRepository;

    @Test
    void duasPrevisoesIguaisNoMesmoHorarioFalhamNoBanco() {
        Localizacao local = localizacaoRepository.saveAndFlush(
                new Localizacao("Cidade Restricao Previsao", -1.0, -2.0, "UTC"));
        Instant quando = Instant.parse("2026-02-01T00:00:00Z");

        previsaoRepository.saveAndFlush(novaPrevisao(local, quando));

        assertThatThrownBy(() -> previsaoRepository.saveAndFlush(novaPrevisao(local, quando)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(previsaoRepository.findByLocation(local, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
    }

    @Test
    void cidadeComCaixaDiferenteFalhaNoBanco() {
        localizacaoRepository.saveAndFlush(
                new Localizacao("Cidade Restricao Caixa", 1.0, 2.0, "UTC"));

        assertThatThrownBy(() -> localizacaoRepository.saveAndFlush(
                new Localizacao("CIDADE RESTRICAO CAIXA", 1.0, 2.0, "UTC")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cidadeComAcentoEmCaixaDiferenteFalhaNoBanco() {
        localizacaoRepository.saveAndFlush(
                new Localizacao("Brasília Restricao", 3.0, 4.0, "UTC"));

        assertThatThrownBy(() -> localizacaoRepository.saveAndFlush(
                new Localizacao("BRASÍLIA RESTRICAO", 3.0, 4.0, "UTC")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static PrevisaoDoCLima novaPrevisao(Localizacao local, Instant quando) {
        return new PrevisaoDoCLima(local, quando, 20, 20, 50, 0, 3, 5, 90, 1010, 10, quando);
    }
}
