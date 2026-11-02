package com.aps.weather.demo.client;

import com.aps.weather.demo.config.FusoHorario;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import com.aps.weather.demo.exception.ApiException;
import com.aps.weather.demo.exception.DadoInvalidoException;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.weather.source", havingValue = "openmeteo", matchIfMissing = true)
public class ClienteClima implements FonteDeClima, FonteDePrevisao {

    private static final String BASE_URL_PADRAO = "https://api.open-meteo.com/v1/forecast";
    private static final int MAX_TENTATIVAS = 3;

    private static final String CAMPOS_ATUAIS = String.join(",",
            "temperature_2m",
            "apparent_temperature",
            "relative_humidity_2m",
            "precipitation",
            "weather_code",
            "wind_speed_10m",
            "wind_direction_10m",
            "pressure_msl",
            "cloud_cover");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final long retryBackoffMs;
    private final FusoHorario fuso;
    private final Clock relogio;

    public ClienteClima(ObjectMapper mapper,
                        @Value("${app.weather.openmeteo.base-url:" + BASE_URL_PADRAO + "}") String baseUrl,
                        @Value("${app.weather.openmeteo.timeout-ms:10000}") long timeoutMs,
                        @Value("${app.weather.openmeteo.retry-backoff-ms:1000}") long retryBackoffMs,
                        FusoHorario fuso,
                        Clock relogio) {
        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.requestTimeout = Duration.ofMillis(timeoutMs);
        this.retryBackoffMs = retryBackoffMs;
        this.fuso = fuso;
        this.relogio = relogio;
    }

    public PrevisaoDoCLima buscar(Localizacao local) throws Exception {
        ValidadorDeLocalizacao.validarCoordenadas(local);

        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("current", CAMPOS_ATUAIS);
        PrevisaoResposta resposta =
                solicitar(local, parametros, PrevisaoResposta.class);

        CondicaoAtual atual = resposta.current;
        ValidadorDeCondicao.validar(atual);

        Instant momentoDaPrevisao = converterHorario(atual.time, fuso.zona(local));

        return new PrevisaoDoCLima(
                local,
                momentoDaPrevisao,
                atual.temperatura,
                atual.sensacaoTermica,
                atual.umidade,
                atual.precipitacao,
                atual.codigoTempo,
                atual.vento,
                atual.direcaoVento,
                atual.pressao,
                atual.nuvens,
                Instant.now(relogio));
    }

    @Override
    public List<PrevisaoHoraria> buscarHorarias(Localizacao local) throws Exception {
        ValidadorDeLocalizacao.validarCoordenadas(local);

        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("hourly", "temperature_2m");
        parametros.put("forecast_days", "1");
        PrevisaoHorariaResposta resposta =
                solicitar(local, parametros, PrevisaoHorariaResposta.class);
        ValidadorDePrevisao.validarHoraria(resposta);

        ZoneId zona = fuso.zona(local);
        Instant atualizadoEm = Instant.now(relogio);
        List<String> horarios = resposta.hourly.time;
        List<Double> temperaturas = resposta.hourly.temperature2m;

        List<PrevisaoHoraria> previsoes = new ArrayList<>(horarios.size());
        for (int i = 0; i < horarios.size(); i++) {
            previsoes.add(new PrevisaoHoraria(
                    local,
                    converterHorario(horarios.get(i), zona),
                    temperaturas.get(i),
                    atualizadoEm));
        }
        return previsoes;
    }

    @Override
    public List<PrevisaoDiaria> buscarDiarias(Localizacao local) throws Exception {
        ValidadorDeLocalizacao.validarCoordenadas(local);
        // A data já vem no fuso da cidade, então não precisa converter.
        fuso.zona(local);

        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("daily", "temperature_2m_min,temperature_2m_max");
        parametros.put("forecast_days", "7");
        PrevisaoDiariaResposta resposta =
                solicitar(local, parametros, PrevisaoDiariaResposta.class);
        ValidadorDePrevisao.validarDiaria(resposta);

        Instant atualizadoEm = Instant.now(relogio);
        List<String> datas = resposta.daily.time;
        List<Double> minimas = resposta.daily.temperature2mMin;
        List<Double> maximas = resposta.daily.temperature2mMax;

        List<PrevisaoDiaria> previsoes = new ArrayList<>(datas.size());
        for (int i = 0; i < datas.size(); i++) {
            previsoes.add(new PrevisaoDiaria(
                    local,
                    converterData(datas.get(i)),
                    minimas.get(i),
                    maximas.get(i),
                    atualizadoEm));
        }
        return previsoes;
    }

    private <T> T solicitar(Localizacao local, Map<String, String> parametros,
                            Class<T> tipo) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(montarUri(local, parametros))
                .timeout(requestTimeout)
                .GET()
                .build();

        HttpResponse<String> response = enviarComTentativas(request);
        return mapper.readValue(response.body(), tipo);
    }

    /**
     * Repete só quando a falha pode ser passageira: sem conexão, timeout e status 5xx.
     * Status 4xx (inclusive 429) não é repetido: repetir não ajuda.
     */
    private HttpResponse<String> enviarComTentativas(HttpRequest request)
            throws IOException, InterruptedException, ApiException {
        for (int tentativa = 1; ; tentativa++) {
            try {
                HttpResponse<String> resposta =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = resposta.statusCode();

                if (status == 200) {
                    return resposta;
                }
                boolean passageiro = status >= 500;
                if (!passageiro || tentativa == MAX_TENTATIVAS) {
                    throw new ApiException(status, resposta.body());
                }
            } catch (ConnectException | HttpTimeoutException e) {
                if (tentativa == MAX_TENTATIVAS) {
                    throw e;
                }
            }
            Thread.sleep(retryBackoffMs * tentativa);   // espera base, depois o dobro
        }
    }

    private Instant converterHorario(String texto, ZoneId zona) throws DadoInvalidoException {
        try {
            return LocalDateTime.parse(texto).atZone(zona).toInstant();
        } catch (DateTimeParseException e) {
            throw new DadoInvalidoException("horário em formato inesperado: " + texto);
        }
    }

    private LocalDate converterData(String texto) throws DadoInvalidoException {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new DadoInvalidoException("data em formato inesperado: " + texto);
        }
    }

    private URI montarUri(Localizacao local, Map<String, String> parametros)
            throws DadoInvalidoException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("latitude", String.valueOf(local.getLatitude()));
        params.put("longitude", String.valueOf(local.getLongitude()));
        params.putAll(parametros);
        params.put("timezone", fuso.id(local));

        String query = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        return URI.create(baseUrl + "?" + query);
    }

    @PreDestroy
    public void close() {
        client.close();
    }
}
