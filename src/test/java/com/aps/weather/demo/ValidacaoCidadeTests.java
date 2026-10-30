package com.aps.weather.demo;

import com.aps.weather.demo.repository.PrevisaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ValidacaoCidadeTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrevisaoRepository previsaoRepository;

    @BeforeEach
    void limparPrevisoes() {
        // A simulação trunca o horário em segundos: uma previsão deixada por outro teste
        // no mesmo segundo colidiria no índice único antes desta coleta.
        previsaoRepository.deleteAll();
    }

    @Test
    void cidadeSoComEspacosEhRejeitadaCom400NosDoisEndpoints() throws Exception {
        mockMvc.perform(get("/api/weather/{city}", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Requisição inválida"));

        mockMvc.perform(post("/api/collect/{city}", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void cidadeAcimaDoLimiteDaColunaEhRejeitadaCom400NosDoisEndpoints() throws Exception {
        String longa = "a".repeat(151);

        mockMvc.perform(get("/api/weather/{city}", longa))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("150")));

        mockMvc.perform(post("/api/collect/{city}", longa))
                .andExpect(status().isBadRequest());
    }

    @Test
    void espacosNasPontasSaoIgnorados() throws Exception {
        mockMvc.perform(post("/api/collect/{city}", " São Paulo "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("São Paulo"));

        mockMvc.perform(get("/api/weather/{city}", "  são paulo  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("São Paulo"));
    }

    @Test
    void maiusculasComAcentoAchamACidadeSemeada() throws Exception {
        mockMvc.perform(post("/api/collect/{city}", "São Paulo"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/weather/{city}", "SÃO PAULO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("São Paulo"));
    }
}
