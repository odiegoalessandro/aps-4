package com.aps.weather.demo;

import com.aps.weather.demo.repository.PrevisaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o que o ticket de observabilidade exige: health com o banco, contadores por
 * cidade e timer da coleta. As métricas se acumulam no contexto compartilhado, então
 * as asserções usam "pelo menos".
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrevisaoRepository previsaoRepository;

    @Test
    void healthReportaOBancoSqliteSemVazarDetalhes() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.db.details").doesNotExist());
    }

    @Test
    void coletaAlimentaContadoresPorCidadeETimer() throws Exception {
        previsaoRepository.deleteAll();

        mockMvc.perform(post("/api/collect"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics/aps.weather.collect.city")
                        .param("tag", "resultado:sucesso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(5.0)))
                .andExpect(jsonPath("$.availableTags[?(@.tag == 'cidade')]").exists());

        mockMvc.perform(get("/actuator/metrics/aps.weather.collect")
                        .param("tag", "tipo:cidade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(5.0)));

        mockMvc.perform(get("/actuator/metrics/aps.weather.collect")
                        .param("tag", "tipo:lote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(1.0)));
    }
}
