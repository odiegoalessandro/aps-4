package com.aps.weather.demo.service;

import com.aps.weather.demo.client.FonteDeClima;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.NomeDeCidade;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.repository.LocalizacaoRepository;
import com.aps.weather.demo.repository.PrevisaoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import tools.jackson.core.JacksonException;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ColetaService {

    private static final Logger LOG = LoggerFactory.getLogger(ColetaService.class);
    private static final int LIMITE_ALERTA = 5;

    private final FonteDeClima fonte;
    private final LocalizacaoRepository localizacaoRepository;
    private final PrevisaoRepository previsaoRepository;
    private final boolean pularRepetidos;
    private final MeterRegistry meterRegistry;
    private final Timer tempoLote;
    private final Timer tempoCidade;

    // cidade -> quantas falhas seguidas
    private final Map<String, Integer> falhasSeguidas = new ConcurrentHashMap<>();

    public ColetaService(FonteDeClima fonte,
                         LocalizacaoRepository localizacaoRepository,
                         PrevisaoRepository previsaoRepository,
                         @Value("${app.weather.collection.pular-repetidos:true}") boolean pularRepetidos,
                         MeterRegistry meterRegistry) {
        this.fonte = fonte;
        this.localizacaoRepository = localizacaoRepository;
        this.previsaoRepository = previsaoRepository;
        this.pularRepetidos = pularRepetidos;
        this.meterRegistry = meterRegistry;
        this.tempoLote = Timer.builder("aps.weather.collect")
                .description("Duração da coleta")
                .tag("tipo", "lote")
                .register(meterRegistry);
        this.tempoCidade = Timer.builder("aps.weather.collect")
                .description("Duração da coleta")
                .tag("tipo", "cidade")
                .register(meterRegistry);
    }

    public ResultadoColeta coletarTodas() {
        Timer.Sample amostra = Timer.start(meterRegistry);
        try {
            List<PrevisaoDoCLima> gravadas = new ArrayList<>();
            List<FalhaColeta> falhas = new ArrayList<>();
            for (Localizacao cidade : localizacaoRepository.findAllByOrderByCityAsc()) {
                try {
                    gravadas.addAll(coletarCidade(cidade));
                } catch (InterruptedException e) {
                    // Interrupção (ex.: desligamento) aborta o lote: continuar só prolongaria
                    // a espera. O flag é reposto para que a thread pare no próximo bloqueio.
                    Thread.currentThread().interrupt();
                    falhas.add(new FalhaColeta(cidade.getCity(), motivo(e)));
                    LOG.warn("Coleta interrompida; as cidades seguintes não foram processadas");
                    break;
                } catch (Exception e) {
                    // coletarCidade já registrou a falha; acumula o motivo e segue para a próxima
                    falhas.add(new FalhaColeta(cidade.getCity(), motivo(e)));
                }
            }
            return new ResultadoColeta(gravadas, falhas);
        } finally {
            amostra.stop(tempoLote);
        }
    }

    public List<PrevisaoDoCLima> coletarCidade(Localizacao cidade) throws Exception {
        String nome = cidade.getCity();
        Timer.Sample amostra = Timer.start(meterRegistry);
        try {
            Localizacao local = garantirLocalizacao(cidade);
            PrevisaoDoCLima previsao = fonte.buscar(local);

            if (pularRepetidos && previsaoRepository.existsByLocationAndForecastAt(local, previsao.getForecastAt())) {
                LOG.info("{}: sem dado novo da API, não gravado", nome);
                registrarSucesso(nome);
                return List.of();
            }

            try {
                previsaoRepository.save(previsao);
            } catch (DataIntegrityViolationException e) {
                // outra coleta gravou o mesmo horário entre o exists e o save
                if (!ehCorridaDePrevisao(e)) {
                    throw e;
                }
                LOG.info("{}: previsão do mesmo horário já gravada por outra coleta", nome);
                registrarSucesso(nome);
                return List.of();
            }
            registrarSucesso(nome);
            return List.of(previsao);
        } catch (Exception e) {
            registrarFalha(nome, e);
            throw e;
        } finally {
            amostra.stop(tempoCidade);
        }
    }

    private Localizacao garantirLocalizacao(Localizacao cidade) {
        String normalizada = NomeDeCidade.normalizar(cidade.getCity());
        return localizacaoRepository.findByCityNormalizada(normalizada)
                .orElseGet(() -> salvarCidade(cidade, normalizada));
    }

    /**
     * Se outra coleta criar a mesma cidade entre a busca e o save, o índice único
     * ux_location_city_normalizada recusa a segunda gravação. Nesse caso usa a cidade
     * que venceu a corrida em vez de propagar o erro.
     */
    private Localizacao salvarCidade(Localizacao cidade, String normalizada) {
        try {
            return localizacaoRepository.saveAndFlush(cidade);
        } catch (DataIntegrityViolationException e) {
            return localizacaoRepository.findByCityNormalizada(normalizada)
                    .orElseThrow(() -> e);
        }
    }

    /**
     * Distingue a violação do índice único de previsões de outras falhas de integridade,
     * que continuam sendo reportadas como erro. A mensagem de erro do SQLite não traz o
     * nome do índice, mas lista todas as colunas dele; exigir as duas evita confundir
     * esta corrida com uma violação de outra constraint que cite {@code location_id}.
     */
    private static boolean ehCorridaDePrevisao(DataIntegrityViolationException e) {
        if (!(e.getMostSpecificCause() instanceof SQLException sql)) {
            return false;
        }
        String mensagem = sql.getMessage();
        return mensagem != null
                && mensagem.contains("UNIQUE constraint failed")
                && mensagem.contains("weather_forecasts.location_id")
                && mensagem.contains("weather_forecasts.forecast_at");
    }

    private void registrarSucesso(String cidade) {
        contar(cidade, "sucesso");
        Integer antes = falhasSeguidas.remove(cidade);
        if (antes != null) {
            LOG.info("{}: coleta normalizada após {} falha(s) seguida(s)", cidade, antes);
        }
    }

    private void registrarFalha(String cidade, Exception e) {
        contar(cidade, "falha");
        int total = falhasSeguidas.merge(cidade, 1, Integer::sum);

        String mensagem = "%s: %s (falha %d seguida(s))".formatted(cidade, motivo(e), total);
        if (total == LIMITE_ALERTA) {
            mensagem += " - FALHA PERSISTENTE";
        }

        if (temRastro(e)) {
            LOG.error(mensagem, e);
        } else {
            LOG.warn(mensagem);
        }
    }

    private void contar(String cidade, String resultado) {
        meterRegistry.counter("aps.weather.collect.city",
                "cidade", cidade,
                "resultado", resultado).increment();
    }

    private static String motivo(Exception e) {
        if (e instanceof InterruptedException) {
            return "coleta interrompida";
        }
        if (e instanceof ConnectException || e instanceof HttpTimeoutException) {
            return "sem conexão com a API ou tempo esgotado";
        }
        if (e instanceof ApiException api) {
            return "API respondeu com status " + api.getStatus() + ": " + resumo(api.getCorpo());
        }
        if (e instanceof DadoInvalidoException || e instanceof JacksonException) {
            return "dados inválidos recebidos: " + e.getMessage();
        }
        if (e instanceof PersistenceException || e instanceof DataAccessException) {
            return "erro no banco de dados";
        }
        return "erro inesperado";
    }

    private static boolean temRastro(Exception e) {
        return e instanceof PersistenceException
                || e instanceof DataAccessException
                || !(e instanceof ConnectException
                || e instanceof HttpTimeoutException
                || e instanceof InterruptedException
                || e instanceof ApiException
                || e instanceof DadoInvalidoException
                || e instanceof JacksonException);
    }

    private static String resumo(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() > 200 ? texto.substring(0, 200) + "..." : texto;
    }
}
