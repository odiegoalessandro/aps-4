package com.aps.weather.demo;

import com.aps.weather.demo.client.ClienteSimulado;
import com.aps.weather.demo.exception.ApiException;
import com.aps.weather.demo.exception.DadoInvalidoException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita cada ramo do ApiExceptionHandler pela rota de coleta, sem rede.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiErrorTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteSimulado fonte;

    @Test
    void dadoInvalidoVira422ComTituloDeDadosInvalidos() throws Exception {
        when(fonte.buscar(any())).thenThrow(new DadoInvalidoException("campo 'current' ausente"));

        mockMvc.perform(post("/api/collect/{city}", "São Paulo"))
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value(containsString("current")));
    }

    @Test
    void falhaDeConexaoVira502() throws Exception {
        when(fonte.buscar(any())).thenThrow(new ConnectException("conexão recusada"));

        mockMvc.perform(post("/api/collect/{city}", "São Paulo"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Falha ao consultar a fonte de clima"))
                .andExpect(jsonPath("$.detail").value(containsString("recusada")));
    }

    @Test
    void timeoutVira502() throws Exception {
        when(fonte.buscar(any())).thenThrow(HttpTimeoutException.class);

        mockMvc.perform(post("/api/collect/{city}", "São Paulo"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Falha ao consultar a fonte de clima"));
    }

    @Test
    void respostaDeErroDaApiVira502ComOStatusDaFonte() throws Exception {
        when(fonte.buscar(any())).thenThrow(new ApiException(500, "serviço fora do ar"));

        mockMvc.perform(post("/api/collect/{city}", "São Paulo"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Falha ao consultar a fonte de clima"))
                .andExpect(jsonPath("$.detail").value(containsString("500")));
    }

    @Test
    void falhaDePersistenciaVira503SemVazarDetalheInterno() throws Exception {
        when(fonte.buscar(any())).thenThrow(new PersistenceException("disco cheio"));

        mockMvc.perform(post("/api/collect/{city}", "São Paulo"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Erro no banco de dados"))
                .andExpect(jsonPath("$.detail")
                        .value("Não foi possível concluir a operação no banco de dados"))
                .andExpect(jsonPath("$.detail").value(not(containsString("disco cheio"))));
    }

    @Test
    void falhaDeAcessoADadosVira503SemVazarDetalheInterno() throws Exception {
        when(fonte.buscar(any())).thenThrow(new DataIntegrityViolationException(
                "UNIQUE constraint failed: weather_forecasts.location_id"));

        mockMvc.perform(post("/api/collect/{city}", "São Paulo"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Erro no banco de dados"))
                .andExpect(jsonPath("$.detail").value(not(containsString("UNIQUE"))));
    }

    @Test
    void interrupcaoVira503ERepoeOFlagDeInterrupcao() throws Exception {
        when(fonte.buscar(any())).thenThrow(new InterruptedException("coleta parada"));

        try {
            mockMvc.perform(post("/api/collect/{city}", "São Paulo"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Coleta interrompida"));

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            // Não vazar a interrupção para os próximos testes.
            Thread.interrupted();
        }
    }
}
