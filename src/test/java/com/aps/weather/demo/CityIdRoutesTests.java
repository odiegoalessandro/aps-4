package com.aps.weather.demo;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.NomeDeCidade;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.repository.LocalizacaoRepository;
import com.aps.weather.demo.repository.PrevisaoDiariaRepository;
import com.aps.weather.demo.repository.PrevisaoHorariaRepository;
import com.aps.weather.demo.repository.PrevisaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rotas equivalentes que identificam a cidade pelo id devolvido em GET /api/cities,
 * sob o segmento /id/{id}, evitando codificar o nome na URL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CityIdRoutesTests {

    private static final String CIDADE = "Rio de Janeiro";
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalizacaoRepository localizacoes;

    @Autowired
    private PrevisaoRepository previsoes;

    @Autowired
    private PrevisaoHorariaRepository horarias;

    @Autowired
    private PrevisaoDiariaRepository diarias;

    @BeforeEach
    void limparDados() {
        previsoes.deleteAll();
        horarias.deleteAll();
        diarias.deleteAll();
    }

    @Test
    void coletaEConsultaPeloId() throws Exception {
        Long id = idDe(CIDADE);

        mockMvc.perform(post("/api/collect/id/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].city").value(CIDADE));

        mockMvc.perform(get("/api/weather/id/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value(CIDADE))
                .andExpect(jsonPath("$.forecastAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/weather/id/{id}/history", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].city").value(CIDADE));
    }

    @Test
    void horariasEDiariasPeloIdMantemOContrato() throws Exception {
        Long id = idDe("São Paulo");
        LocalDate hoje = LocalDate.now(ZONA);

        mockMvc.perform(get("/api/weather/id/{id}/hourly", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date").value(hoje.toString()))
                .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.temperatures.length()").value(24));

        mockMvc.perform(get("/api/weather/id/{id}/daily", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecast.length()").value(7))
                .andExpect(jsonPath("$.forecast[0].date").value(hoje.toString()));
    }

    @Test
    void historicoPeloIdRespeitaPaginacao() throws Exception {
        Localizacao local = localizacoes.saveAndFlush(
                new Localizacao("Cidade Id Paginacao", -20.0, -40.0, "America/Sao_Paulo"));
        for (int hora = 10; hora <= 14; hora++) {
            Instant momento = Instant.parse("2026-10-30T%02d:00:00Z".formatted(hora));
            previsoes.saveAndFlush(new PrevisaoDoCLima(
                    local, momento, 20.0, 19.0, 60, 0.0, 0,
                    10.0, 90.0, 1010.0, 20.0, momento));
        }

        mockMvc.perform(get("/api/weather/id/{id}/history", local.getId())
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content[0].forecastAt").value("2026-10-30T12:00:00Z"))
                .andExpect(jsonPath("$.content[1].forecastAt").value("2026-10-30T11:00:00Z"));
    }

    @Test
    void parametrosDePaginacaoInvalidosPeloIdRetornam400() throws Exception {
        Long id = idDe(CIDADE);

        mockMvc.perform(get("/api/weather/id/{id}/history", id).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/weather/id/{id}/history", id).param("size", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void idDesconhecidoRetorna404EmTodasAsRotas() throws Exception {
        long id = 999_999L;

        mockMvc.perform(get("/api/weather/id/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/weather/id/{id}/history", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/weather/id/{id}/hourly", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/weather/id/{id}/daily", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post("/api/collect/id/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void idNaoNumericoRetorna400() throws Exception {
        mockMvc.perform(get("/api/weather/id/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private Long idDe(String cidade) {
        return localizacoes.findByCityNormalizada(NomeDeCidade.normalizar(cidade))
                .orElseThrow()
                .getId();
    }
}
