package com.aps.weather.demo;

import com.aps.weather.demo.domain.NomeDeCidade;
import com.aps.weather.demo.repository.LocalizacaoRepository;
import com.aps.weather.demo.repository.PrevisaoDiariaRepository;
import com.aps.weather.demo.repository.PrevisaoHorariaRepository;
import com.aps.weather.demo.repository.PrevisaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documenta as rotas com Spring REST Docs. Os snippets gerados em
 * target/generated-snippets alimentam src/docs/asciidoc/index.adoc, transformado
 * em HTML pelo plugin do Asciidoctor durante o build.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class ApiDocumentationTests {

    private static final String CIDADE = "São Paulo";

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
    void listarCidades() throws Exception {
        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andDo(document("cities/listar",
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].id").type(NUMBER)
                                        .description("Identificador da cidade"),
                                fieldWithPath("[].city").type(STRING)
                                        .description("Nome da cidade"),
                                fieldWithPath("[].latitude").type(NUMBER)
                                        .description("Latitude em graus"),
                                fieldWithPath("[].longitude").type(NUMBER)
                                        .description("Longitude em graus"),
                                fieldWithPath("[].timezone").type(STRING)
                                        .description("Fuso IANA da cidade"))));
    }

    @Test
    void climaAtual() throws Exception {
        coletar(CIDADE);

        mockMvc.perform(get("/api/weather/{city}", CIDADE))
                .andExpect(status().isOk())
                .andDo(document("weather/atual",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("city")
                                .description("Nome da cidade cadastrada")),
                        responseFields(camposDaPrevisao(""))));
    }

    @Test
    void historicoPaginado() throws Exception {
        coletar(CIDADE);

        mockMvc.perform(get("/api/weather/{city}/history", CIDADE)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("weather/historico",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("city")
                                .description("Nome da cidade cadastrada")),
                        queryParameters(
                                parameterWithName("page").optional()
                                        .description("Página, base zero (padrão 0)"),
                                parameterWithName("size").optional()
                                        .description("Itens por página (padrão 50, máximo 200)")),
                        responseFields(camposDoHistorico())));
    }

    @Test
    void temperaturasHorarias() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/hourly", CIDADE))
                .andExpect(status().isOk())
                .andDo(document("weather/horarias",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("city")
                                .description("Nome da cidade cadastrada")),
                        responseFields(camposHorarias())));
    }

    @Test
    void temperaturasDiarias() throws Exception {
        mockMvc.perform(get("/api/weather/{city}/daily", CIDADE))
                .andExpect(status().isOk())
                .andDo(document("weather/diarias",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("city")
                                .description("Nome da cidade cadastrada")),
                        responseFields(camposDiarias())));
    }

    @Test
    void coletarTodasAsCidades() throws Exception {
        List<FieldDescriptor> campos = new ArrayList<>();
        campos.add(fieldWithPath("gravadas").type(ARRAY)
                .description("Previsões gravadas com sucesso"));
        campos.addAll(Arrays.asList(camposDaPrevisao("gravadas[].")));
        campos.add(fieldWithPath("falhas").type(ARRAY)
                .description("Cidades cuja coleta falhou"));
        campos.add(fieldWithPath("falhas[].cidade").type(STRING).optional()
                .description("Cidade com falha"));
        campos.add(fieldWithPath("falhas[].motivo").type(STRING).optional()
                .description("Motivo legível da falha"));

        mockMvc.perform(post("/api/collect"))
                .andExpect(status().isOk())
                .andDo(document("collect/todas",
                        preprocessResponse(prettyPrint()),
                        responseFields(campos)));
    }

    @Test
    void coletarUmaCidade() throws Exception {
        mockMvc.perform(post("/api/collect/{city}", CIDADE))
                .andExpect(status().isOk())
                .andDo(document("collect/cidade",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("city")
                                .description("Nome da cidade cadastrada")),
                        responseFields(camposDaPrevisao("[]."))));
    }

    @Test
    void climaAtualPorId() throws Exception {
        Long id = cidadeId();
        coletarPorId(id);

        mockMvc.perform(get("/api/cities/{id}/weather", id))
                .andExpect(status().isOk())
                .andDo(document("cities/weather-por-id",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("id")
                                .description("Identificador da cidade")),
                        responseFields(camposDaPrevisao(""))));
    }

    @Test
    void historicoPaginadoPorId() throws Exception {
        Long id = cidadeId();
        coletarPorId(id);

        mockMvc.perform(get("/api/cities/{id}/history", id)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("cities/historico-por-id",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("id")
                                .description("Identificador da cidade")),
                        queryParameters(
                                parameterWithName("page").optional()
                                        .description("Página, base zero (padrão 0)"),
                                parameterWithName("size").optional()
                                        .description("Itens por página (padrão 50, máximo 200)")),
                        responseFields(camposDoHistorico())));
    }

    @Test
    void temperaturasHorariasPorId() throws Exception {
        Long id = cidadeId();

        mockMvc.perform(get("/api/cities/{id}/hourly", id))
                .andExpect(status().isOk())
                .andDo(document("cities/horarias-por-id",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("id")
                                .description("Identificador da cidade")),
                        responseFields(camposHorarias())));
    }

    @Test
    void temperaturasDiariasPorId() throws Exception {
        Long id = cidadeId();

        mockMvc.perform(get("/api/cities/{id}/daily", id))
                .andExpect(status().isOk())
                .andDo(document("cities/diarias-por-id",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("id")
                                .description("Identificador da cidade")),
                        responseFields(camposDiarias())));
    }

    @Test
    void coletarUmaCidadePorId() throws Exception {
        Long id = cidadeId();

        mockMvc.perform(post("/api/cities/{id}/collect", id))
                .andExpect(status().isOk())
                .andDo(document("cities/coletar-por-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("id")
                                .description("Identificador da cidade")),
                        responseFields(camposDaPrevisao("[]."))));
    }

    private Long cidadeId() {
        return localizacoes.findByCityNormalizada(NomeDeCidade.normalizar(CIDADE))
                .orElseThrow()
                .getId();
    }

    private void coletar(String cidade) throws Exception {
        mockMvc.perform(post("/api/collect/{city}", cidade))
                .andExpect(status().isOk());
    }

    private void coletarPorId(Long id) throws Exception {
        mockMvc.perform(post("/api/cities/{id}/collect", id))
                .andExpect(status().isOk());
    }

    private static FieldDescriptor[] camposDoHistorico() {
        List<FieldDescriptor> campos = new ArrayList<>();
        campos.add(fieldWithPath("content").type(ARRAY)
                .description("Previsões da página atual"));
        campos.addAll(Arrays.asList(camposDaPrevisao("content[].")));
        campos.add(fieldWithPath("page").type(NUMBER)
                .description("Página atual, base zero"));
        campos.add(fieldWithPath("size").type(NUMBER)
                .description("Tamanho solicitado da página"));
        campos.add(fieldWithPath("totalElements").type(NUMBER)
                .description("Total de previsões da cidade"));
        campos.add(fieldWithPath("totalPages").type(NUMBER)
                .description("Total de páginas"));
        return campos.toArray(new FieldDescriptor[0]);
    }

    private static FieldDescriptor[] camposHorarias() {
        return new FieldDescriptor[] {
                fieldWithPath("date").type(STRING)
                        .description("Data de referência no fuso da cidade"),
                fieldWithPath("timezone").type(STRING)
                        .description("Fuso IANA usado na resposta"),
                fieldWithPath("temperatures").type(ARRAY)
                        .description("As 24 temperaturas do dia, em ordem"),
                fieldWithPath("temperatures[].time").type(STRING)
                        .description("Horário local no formato HH:mm"),
                fieldWithPath("temperatures[].temperature").type(NUMBER)
                        .description("Temperatura prevista em °C")
        };
    }

    private static FieldDescriptor[] camposDiarias() {
        return new FieldDescriptor[] {
                fieldWithPath("timezone").type(STRING)
                        .description("Fuso IANA usado na resposta"),
                fieldWithPath("forecast").type(ARRAY)
                        .description("Sete dias a partir de hoje, em ordem"),
                fieldWithPath("forecast[].date").type(STRING)
                        .description("Data local no formato ISO-8601"),
                fieldWithPath("forecast[].minTemperature").type(NUMBER)
                        .description("Temperatura mínima prevista em °C"),
                fieldWithPath("forecast[].maxTemperature").type(NUMBER)
                        .description("Temperatura máxima prevista em °C")
        };
    }

    private static FieldDescriptor[] camposDaPrevisao(String prefixo) {
        return new FieldDescriptor[] {
                fieldWithPath(prefixo + "id").type(NUMBER)
                        .description("Identificador da previsão"),
                fieldWithPath(prefixo + "city").type(STRING)
                        .description("Cidade da previsão"),
                fieldWithPath(prefixo + "forecastAt").type(STRING)
                        .description("Instante previsto em ISO-8601 (UTC)"),
                fieldWithPath(prefixo + "temperature").type(NUMBER)
                        .description("Temperatura em °C"),
                fieldWithPath(prefixo + "apparentTemperature").type(NUMBER)
                        .description("Sensação térmica em °C"),
                fieldWithPath(prefixo + "humidity").type(NUMBER)
                        .description("Umidade relativa em %"),
                fieldWithPath(prefixo + "precipitation").type(NUMBER)
                        .description("Precipitação em mm"),
                fieldWithPath(prefixo + "weatherCode").type(NUMBER)
                        .description("Código WMO do tempo"),
                fieldWithPath(prefixo + "condition").type(STRING)
                        .description("Condição normalizada"),
                fieldWithPath(prefixo + "conditionDescription").type(STRING)
                        .description("Descrição legível da condição"),
                fieldWithPath(prefixo + "windSpeed").type(NUMBER)
                        .description("Velocidade do vento em km/h"),
                fieldWithPath(prefixo + "windDirection").type(NUMBER)
                        .description("Direção do vento em graus"),
                fieldWithPath(prefixo + "pressure").type(NUMBER)
                        .description("Pressão em hPa"),
                fieldWithPath(prefixo + "cloudCover").type(NUMBER)
                        .description("Nebulosidade em %"),
                fieldWithPath(prefixo + "updatedAt").type(STRING)
                        .description("Instante da coleta em ISO-8601 (UTC)")
        };
    }
}
