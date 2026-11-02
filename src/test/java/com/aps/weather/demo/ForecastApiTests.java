package com.aps.weather.demo;

import com.aps.weather.demo.repository.PrevisaoDiariaRepository;
import com.aps.weather.demo.repository.PrevisaoHorariaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ForecastApiTests {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrevisaoHorariaRepository horarias;

    @Autowired
    private PrevisaoDiariaRepository diarias;

    @BeforeEach
    void limparSnapshots() {
        horarias.deleteAll();
        diarias.deleteAll();
    }

    @Test
    void horariasRetornamAs24HorasDoDiaAtual() throws Exception {
        LocalDate hoje = LocalDate.now(ZONA);

        mockMvc.perform(get("/api/weather/{city}/hourly", "São Paulo"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date").value(hoje.toString()))
                .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.temperatures.length()").value(24))
                .andExpect(jsonPath("$.temperatures[0].time").value("00:00"))
                .andExpect(jsonPath("$.temperatures[23].time").value("23:00"))
                .andExpect(jsonPath("$.temperatures[0].temperature").isNumber());

        assertThat(horarias.count()).isEqualTo(24);
    }

    @Test
    void diariasRetornamSeteDiasAPartirDeHoje() throws Exception {
        LocalDate hoje = LocalDate.now(ZONA);

        mockMvc.perform(get("/api/weather/{city}/daily", "São Paulo"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.forecast.length()").value(7))
                .andExpect(jsonPath("$.forecast[0].date").value(hoje.toString()))
                .andExpect(jsonPath("$.forecast[6].date").value(hoje.plusDays(6).toString()))
                .andExpect(jsonPath("$.forecast[0].minTemperature").isNumber())
                .andExpect(jsonPath("$.forecast[0].maxTemperature").isNumber());

        assertThat(diarias.count()).isEqualTo(7);
    }

    @Test
    void segundaConsultaReaproveitaOSnapshotPersistido() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/hourly", "São Paulo"))
                .andExpect(status().isOk());
        var atualizadoEm = horarias.findAll().get(0).getUpdatedAt();

        mockMvc.perform(get("/api/weather/{city}/hourly", "São Paulo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperatures.length()").value(24));

        assertThat(horarias.count()).isEqualTo(24);
        assertThat(horarias.findAll())
                .allMatch(previsao -> previsao.getUpdatedAt().equals(atualizadoEm));
    }

    @Test
    void cidadeDesconhecidaRetorna404NosDoisEndpoints() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/hourly", "Atlântida"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/weather/{city}/daily", "Atlântida"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
