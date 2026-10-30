package com.aps.weather.demo;

import com.aps.weather.demo.repository.PrevisaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WeatherApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrevisaoRepository previsaoRepository;

    @Test
    void fluxoCompletoDaApi() throws Exception {
        previsaoRepository.deleteAll();

        mockMvc.perform(get("/api/weather/{city}", "São Paulo"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post("/api/collect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gravadas.length()").value(5))
                .andExpect(jsonPath("$.falhas.length()").value(0));

        mockMvc.perform(get("/api/weather/{city}", "São Paulo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("São Paulo"))
                .andExpect(jsonPath("$.conditionDescription").value("Tempestade"))
                .andExpect(jsonPath("$.forecastAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get("/api/weather/{city}/history", "São Paulo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].city").value("São Paulo"));

        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        // O simulador trunca o horário em segundos: duas coletas no mesmo segundo
        // colidiriam no índice único, então limpamos antes da coleta individual.
        previsaoRepository.deleteAll();

        mockMvc.perform(post("/api/collect/{city}", "Rio de Janeiro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].city").value("Rio de Janeiro"));

        mockMvc.perform(get("/api/weather/{city}", "Atlântida"))
                .andExpect(status().isNotFound());
    }
}
