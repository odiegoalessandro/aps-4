package com.aps.weather.demo.client;

import com.aps.weather.demo.config.FusoHorario;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import com.aps.weather.demo.exception.ApiException;
import com.aps.weather.demo.exception.DadoInvalidoException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercita o cliente contra um servidor HTTP do JDK em porta efêmera.
 * Nenhum teste acessa a internet.
 */
class ClienteClimaTest {

    private static final Localizacao SAO_PAULO =
            new Localizacao("São Paulo", -23.55, -46.63, "America/Sao_Paulo");

    private static final FusoHorario FUSO = new FusoHorario("America/Sao_Paulo");
    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-10-30T18:00:00Z"), ZoneOffset.UTC);

    private static final String CORPO_VALIDO = """
            {
              "latitude": -23.55,
              "longitude": -46.63,
              "timezone": "America/Sao_Paulo",
              "current": {
                "time": "2026-10-30T15:00",
                "temperature_2m": 24.5,
                "apparent_temperature": 25.0,
                "relative_humidity_2m": 55,
                "precipitation": 0.0,
                "weather_code": 3,
                "wind_speed_10m": 8.0,
                "wind_direction_10m": 180.0,
                "pressure_msl": 1015.0,
                "cloud_cover": 15.0
              }
            }
            """;

    private static final String CORPO_HORARIO = """
            {
              "timezone": "America/Sao_Paulo",
              "hourly": {
                "time": ["2026-10-30T00:00", "2026-10-30T01:00", "2026-10-30T02:00",
                         "2026-10-30T03:00", "2026-10-30T04:00", "2026-10-30T05:00",
                         "2026-10-30T06:00", "2026-10-30T07:00", "2026-10-30T08:00",
                         "2026-10-30T09:00", "2026-10-30T10:00", "2026-10-30T11:00",
                         "2026-10-30T12:00", "2026-10-30T13:00", "2026-10-30T14:00",
                         "2026-10-30T15:00", "2026-10-30T16:00", "2026-10-30T17:00",
                         "2026-10-30T18:00", "2026-10-30T19:00", "2026-10-30T20:00",
                         "2026-10-30T21:00", "2026-10-30T22:00", "2026-10-30T23:00"],
                "temperature_2m": [19.5, 19.2, 20.0, 20.0, 20.0, 20.0, 20.0, 20.0,
                                   20.0, 20.0, 20.0, 20.0, 20.0, 20.0, 20.0, 20.0,
                                   20.0, 20.0, 20.0, 20.0, 20.0, 20.0, 20.0, 20.0]
              }
            }
            """;

    private static final String CORPO_DIARIO = """
            {
              "timezone": "America/Sao_Paulo",
              "daily": {
                "time": ["2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05",
                         "2026-11-06", "2026-11-07", "2026-11-08"],
                "temperature_2m_min": [18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0],
                "temperature_2m_max": [27.0, 30.0, 31.0, 32.0, 33.0, 34.0, 35.0]
              }
            }
            """;

    private static final Instant HORARIO_ESPERADO = LocalDateTime.parse("2026-10-30T15:00")
            .atZone(ZoneId.of("America/Sao_Paulo"))
            .toInstant();

    private final AtomicInteger chamadas = new AtomicInteger();
    private volatile URI ultimaUri;
    private HttpServer servidor;
    private ClienteClima cliente;

    @AfterEach
    void encerrar() {
        if (cliente != null) {
            cliente.close();
        }
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void jsonValidoEhConvertidoParaPrevisao() throws Exception {
        String baseUrl = subirServidor(200, CORPO_VALIDO, 0);
        cliente = novoCliente(baseUrl, 10_000);

        PrevisaoDoCLima previsao = cliente.buscar(SAO_PAULO);

        assertThat(previsao.getLocation()).isSameAs(SAO_PAULO);
        assertThat(previsao.getTemperature()).isEqualTo(24.5);
        assertThat(previsao.getApparentTemperature()).isEqualTo(25.0);
        assertThat(previsao.getHumidity()).isEqualTo(55);
        assertThat(previsao.getPrecipitation()).isEqualTo(0.0);
        assertThat(previsao.getWeatherCode()).isEqualTo(3);
        assertThat(previsao.getWindSpeed()).isEqualTo(8.0);
        assertThat(previsao.getWindDirection()).isEqualTo(180.0);
        assertThat(previsao.getPressure()).isEqualTo(1015.0);
        assertThat(previsao.getCloudCover()).isEqualTo(15.0);
        assertThat(previsao.getForecastAt()).isEqualTo(HORARIO_ESPERADO);
        assertThat(previsao.getUpdatedAt()).isEqualTo(RELOGIO.instant());
    }

    @Test
    void requisicaoEnviaCoordenadasCamposAtuaisECidade() throws Exception {
        String baseUrl = subirServidor(200, CORPO_VALIDO, 0);
        cliente = novoCliente(baseUrl, 10_000);

        cliente.buscar(SAO_PAULO);

        assertThat(ultimaUri.getPath()).isEqualTo("/v1/forecast");
        assertThat(ultimaUri.getQuery())
                .contains("latitude=-23.55")
                .contains("longitude=-46.63")
                .contains("timezone=America/Sao_Paulo")
                .contains("current=temperature_2m,apparent_temperature,relative_humidity_2m,"
                        + "precipitation,weather_code,wind_speed_10m,wind_direction_10m,"
                        + "pressure_msl,cloud_cover");
    }

    @Test
    void horariasSaoConvertidasNoFusoDaCidade() throws Exception {
        String baseUrl = subirServidor(200, CORPO_HORARIO, 0);
        cliente = novoCliente(baseUrl, 10_000);

        List<PrevisaoHoraria> previsoes = cliente.buscarHorarias(SAO_PAULO);

        assertThat(previsoes).hasSize(24);
        assertThat(previsoes.get(0).getForecastAt()).isEqualTo(
                LocalDateTime.parse("2026-10-30T00:00")
                        .atZone(ZoneId.of("America/Sao_Paulo")).toInstant());
        assertThat(previsoes.get(0).getTemperature()).isEqualTo(19.5);
        assertThat(previsoes.get(1).getTemperature()).isEqualTo(19.2);
        assertThat(previsoes.get(0).getLocation()).isSameAs(SAO_PAULO);
        assertThat(previsoes.get(0).getUpdatedAt()).isEqualTo(RELOGIO.instant());
        assertThat(ultimaUri.getQuery())
                .contains("hourly=temperature_2m")
                .contains("forecast_days=1")
                .contains("timezone=America/Sao_Paulo");
    }

    @Test
    void horariasComArraysDeTamanhosDiferentesSaoInvalidas() throws Exception {
        String baseUrl = subirServidor(200, """
                {
                  "hourly": {
                    "time": ["2026-10-30T00:00", "2026-10-30T01:00"],
                    "temperature_2m": [19.5]
                  }
                }
                """, 0);
        cliente = novoCliente(baseUrl, 10_000);

        assertThatThrownBy(() -> cliente.buscarHorarias(SAO_PAULO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("difere");
    }

    @Test
    void horariasVaziasSaoInvalidas() throws Exception {
        String baseUrl = subirServidor(200, """
                {"hourly": {"time": [], "temperature_2m": []}}
                """, 0);
        cliente = novoCliente(baseUrl, 10_000);

        assertThatThrownBy(() -> cliente.buscarHorarias(SAO_PAULO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("vazia");
    }

    @Test
    void horarioMaiorQueODiaSaoInvalidos() throws Exception {
        String baseUrl = subirServidor(200, """
                {
                  "hourly": {
                    "time": ["2026-10-30T25:00", "2026-10-30T01:00", "2026-10-30T02:00",
                             "2026-10-30T03:00", "2026-10-30T04:00", "2026-10-30T05:00",
                             "2026-10-30T06:00", "2026-10-30T07:00", "2026-10-30T08:00",
                             "2026-10-30T09:00", "2026-10-30T10:00", "2026-10-30T11:00",
                             "2026-10-30T12:00", "2026-10-30T13:00", "2026-10-30T14:00",
                             "2026-10-30T15:00", "2026-10-30T16:00", "2026-10-30T17:00",
                             "2026-10-30T18:00", "2026-10-30T19:00", "2026-10-30T20:00",
                             "2026-10-30T21:00", "2026-10-30T22:00", "2026-10-30T23:00"],
                    "temperature_2m": [19.5, 19.5, 19.5, 19.5, 19.5, 19.5, 19.5, 19.5,
                                       19.5, 19.5, 19.5, 19.5, 19.5, 19.5, 19.5, 19.5,
                                       19.5, 19.5, 19.5, 19.5, 19.5, 19.5, 19.5, 19.5]
                  }
                }
                """, 0);
        cliente = novoCliente(baseUrl, 10_000);

        assertThatThrownBy(() -> cliente.buscarHorarias(SAO_PAULO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("formato inesperado");
    }

    @Test
    void diariasSaoConvertidasComDataMinimaEMaxima() throws Exception {
        String baseUrl = subirServidor(200, CORPO_DIARIO, 0);
        cliente = novoCliente(baseUrl, 10_000);

        List<PrevisaoDiaria> previsoes = cliente.buscarDiarias(SAO_PAULO);

        assertThat(previsoes).hasSize(7);
        assertThat(previsoes.get(0).getForecastDate()).isEqualTo(LocalDate.parse("2026-11-02"));
        assertThat(previsoes.get(0).getMinTemperature()).isEqualTo(18.0);
        assertThat(previsoes.get(0).getMaxTemperature()).isEqualTo(27.0);
        assertThat(previsoes.get(1).getForecastDate()).isEqualTo(LocalDate.parse("2026-11-03"));
        assertThat(previsoes.get(0).getUpdatedAt()).isEqualTo(RELOGIO.instant());
        assertThat(ultimaUri.getQuery())
                .contains("daily=temperature_2m_min,temperature_2m_max")
                .contains("forecast_days=7")
                .contains("timezone=America/Sao_Paulo");
    }

    @Test
    void diariaComMinimaMaiorQueMaximaEhInvalida() throws Exception {
        String baseUrl = subirServidor(200, """
                {
                  "daily": {
                    "time": ["2026-11-02"],
                    "temperature_2m_min": [30.0],
                    "temperature_2m_max": [20.0]
                  }
                }
                """, 0);
        cliente = novoCliente(baseUrl, 10_000);

        assertThatThrownBy(() -> cliente.buscarDiarias(SAO_PAULO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("mínima maior");
    }

    @Test
    void diariaSemBlocoDailyEhInvalida() throws Exception {
        String baseUrl = subirServidor(200, "{\"timezone\":\"America/Sao_Paulo\"}", 0);
        cliente = novoCliente(baseUrl, 10_000);

        assertThatThrownBy(() -> cliente.buscarDiarias(SAO_PAULO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessageContaining("'daily'");
    }

    @Test
    void status500EhRepetidoELancaApiException() throws Exception {
        String baseUrl = subirServidor(500, "{\"erro\":\"interno\"}", 0);
        cliente = novoCliente(baseUrl, 10_000);

        assertThatThrownBy(() -> cliente.buscar(SAO_PAULO))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(500));
        assertThat(chamadas).hasValue(3);
    }

    @Test
    void status429NaoEhRepetido() throws Exception {
        String baseUrl = subirServidor(429, "{\"erro\":\"limite\"}", 0);
        cliente = novoCliente(baseUrl, 10_000);

        assertThatThrownBy(() -> cliente.buscar(SAO_PAULO))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(429));
        assertThat(chamadas).hasValue(1);
    }

    @Test
    void corpoMalformadoLancaJacksonException() throws Exception {
        String baseUrl = subirServidor(200, "isto não é JSON", 0);
        cliente = novoCliente(baseUrl, 10_000);

        assertThatThrownBy(() -> cliente.buscar(SAO_PAULO))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void timeoutLancaHttpTimeoutException() throws Exception {
        String baseUrl = subirServidor(200, CORPO_VALIDO, 1_000);
        cliente = novoCliente(baseUrl, 200);

        assertThatThrownBy(() -> cliente.buscar(SAO_PAULO))
                .isInstanceOf(HttpTimeoutException.class);
    }

    private ClienteClima novoCliente(String baseUrl, long timeoutMs) {
        return new ClienteClima(new JsonMapper(), baseUrl, timeoutMs, 10, FUSO, RELOGIO);
    }

    private String subirServidor(int status, String corpo, long atrasoMs) throws IOException {
        chamadas.set(0);
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "openmeteo-falso");
            thread.setDaemon(true);
            return thread;
        }));
        servidor.createContext("/v1/forecast", exchange -> {
            chamadas.incrementAndGet();
            ultimaUri = exchange.getRequestURI();
            try {
                if (atrasoMs > 0) {
                    Thread.sleep(atrasoMs);
                }
                byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                try (OutputStream saida = exchange.getResponseBody()) {
                    saida.write(bytes);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                // o cliente pode ter desistido antes da resposta (caso de timeout)
            } finally {
                exchange.close();
            }
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1/forecast";
    }
}
