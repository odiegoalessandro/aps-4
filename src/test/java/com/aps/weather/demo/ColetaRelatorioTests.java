package com.aps.weather.demo;

import com.aps.weather.demo.client.ClienteSimulado;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.repository.LocalizacaoRepository;
import com.aps.weather.demo.repository.PrevisaoRepository;
import com.aps.weather.demo.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ColetaRelatorioTests {

    private static final Instant MOMENTO = Instant.parse("2026-10-30T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalizacaoRepository localizacaoRepository;

    @Autowired
    private PrevisaoRepository previsaoRepository;

    @MockitoBean
    private ClienteSimulado fonte;

    @BeforeEach
    void limparPrevisoes() {
        previsaoRepository.deleteAll();
    }

    @Test
    void coletaSemFalhasResponde200ComAsGravadas() throws Exception {
        when(fonte.buscar(any())).thenAnswer(inv -> previsao(inv.getArgument(0)));

        mockMvc.perform(post("/api/collect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gravadas.length()")
                        .value((int) localizacaoRepository.count()))
                .andExpect(jsonPath("$.falhas.length()").value(0));
    }

    @Test
    void coletaParcialResponde207ComGravadasEFalhas() throws Exception {
        when(fonte.buscar(any())).thenAnswer(inv -> previsao(inv.getArgument(0)));
        when(fonte.buscar(argThat(local -> "Rio de Janeiro".equals(local.getCity()))))
                .thenThrow(new ApiException(500, "indisponível"));

        mockMvc.perform(post("/api/collect"))
                .andExpect(status().is(207))
                .andExpect(jsonPath("$.gravadas.length()")
                        .value((int) localizacaoRepository.count() - 1))
                .andExpect(jsonPath("$.falhas.length()").value(1))
                .andExpect(jsonPath("$.falhas[0].cidade").value("Rio de Janeiro"))
                .andExpect(jsonPath("$.falhas[0].motivo").value(containsString("status 500")));
    }

    @Test
    void coletaComTodasAsFalhasResponde502() throws Exception {
        when(fonte.buscar(any())).thenThrow(new ApiException(503, "fora do ar"));

        mockMvc.perform(post("/api/collect"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.gravadas.length()").value(0))
                .andExpect(jsonPath("$.falhas.length()")
                        .value((int) localizacaoRepository.count()))
                .andExpect(jsonPath("$.falhas[0].cidade").isNotEmpty())
                .andExpect(jsonPath("$.falhas[0].motivo").value(containsString("status 503")));
    }

    private static PrevisaoDoCLima previsao(Localizacao local) {
        return new PrevisaoDoCLima(local, MOMENTO, 20, 20, 50, 0, 3, 5, 90, 1010, 10, MOMENTO);
    }
}
