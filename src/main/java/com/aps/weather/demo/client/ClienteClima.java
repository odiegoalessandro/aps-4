package com.aps.weather.demo.client;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.service.ApiException;
import com.aps.weather.demo.service.DadoInvalidoException;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.weather.source", havingValue = "openmeteo", matchIfMissing = true)
public class ClienteClima implements FonteDeClima {

    private static final String BASE_URL_PADRAO = "https://api.open-meteo.com/v1/forecast";
    private static final int MAX_TENTATIVAS = 3;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final long retryBackoffMs;

    public ClienteClima(ObjectMapper mapper,
                        @Value("${app.weather.openmeteo.base-url:" + BASE_URL_PADRAO + "}") String baseUrl,
                        @Value("${app.weather.openmeteo.timeout-ms:10000}") long timeoutMs,
                        @Value("${app.weather.openmeteo.retry-backoff-ms:1000}") long retryBackoffMs) {
        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.requestTimeout = Duration.ofMillis(timeoutMs);
        this.retryBackoffMs = retryBackoffMs;
    }

    public PrevisaoDoCLima buscar(Localizacao local) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(montarUri(local))
                .timeout(requestTimeout)
                .GET()
                .build();

        HttpResponse<String> response = enviarComTentativas(request);

        PrevisaoResposta resposta = mapper.readValue(response.body(), PrevisaoResposta.class);
        if (resposta == null) {
            throw new DadoInvalidoException("resposta vazia da API");
        }
        CondicaoAtual atual = resposta.current;
        ValidadorDeCondicao.validar(atual);

        Instant momentoDaPrevisao = converterHorario(atual.time, local);

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
                Instant.now());
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

    private Instant converterHorario(String texto, Localizacao local) throws DadoInvalidoException {
        try {
            return LocalDateTime.parse(texto)
                    .atZone(ZoneId.of(local.getTimezone()))
                    .toInstant();
        } catch (DateTimeParseException e) {
            throw new DadoInvalidoException("horário em formato inesperado: " + texto);
        }
    }

    private URI montarUri(Localizacao local) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("latitude", String.valueOf(local.getLatitude()));
        params.put("longitude", String.valueOf(local.getLongitude()));
        params.put("current", String.join(",",
                "temperature_2m",
                "apparent_temperature",
                "relative_humidity_2m",
                "precipitation",
                "weather_code",
                "wind_speed_10m",
                "wind_direction_10m",
                "pressure_msl",
                "cloud_cover"));
        params.put("timezone", local.getTimezone());

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
