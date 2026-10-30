package com.aps.weather.demo;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.repository.LocalizacaoRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistoricoPaginacaoTests {

    private static final String CIDADE = "Cidade Paginacao";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalizacaoRepository localizacaoRepository;

    @Autowired
    private PrevisaoRepository previsaoRepository;

    @BeforeEach
    void prepararHistorico() {
        Localizacao local = localizacaoRepository.saveAndFlush(
                new Localizacao(CIDADE, -20.0, -40.0, "America/Sao_Paulo"));

        for (int hora = 10; hora <= 14; hora++) {
            Instant momento = Instant.parse("2026-10-30T%02d:00:00Z".formatted(hora));
            previsaoRepository.saveAndFlush(new PrevisaoDoCLima(
                    local, momento, 20.0, 19.0, 60, 0.0, 0,
                    10.0, 90.0, 1010.0, 20.0, momento));
        }
    }

    @Test
    void paginaInformadaDevolveSomenteOTamanhoPedidoComOsTotais() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/history", CIDADE)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].forecastAt").value("2026-10-30T14:00:00Z"))
                .andExpect(jsonPath("$.content[1].forecastAt").value("2026-10-30T13:00:00Z"));
    }

    @Test
    void segundaPaginaTrazOsItensSeguintesEmOrdemDecrescente() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/history", CIDADE)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content[0].forecastAt").value("2026-10-30T12:00:00Z"))
                .andExpect(jsonPath("$.content[1].forecastAt").value("2026-10-30T11:00:00Z"));
    }

    @Test
    void semParametrosUsaPaginaZeroETamanhoCinquenta() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/history", CIDADE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void tamanhoAcimaDoTetoEhLimitadoADuzentos() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/history", CIDADE)
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(200))
                .andExpect(jsonPath("$.content.length()").value(5));
    }

    @Test
    void pageNegativoEhRejeitadoCom400() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/history", CIDADE)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void sizeNegativoEhRejeitadoCom400() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/history", CIDADE)
                        .param("size", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    void parametroNaoNumericoEhRejeitadoCom400NoFormatoProblemJson() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/history", CIDADE)
                        .param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void cidadeSemDadosContinua404() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/history", "Cidade Inexistente Paginacao"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
