package com.aps.weather.demo.service;

import com.aps.weather.demo.client.FonteDePrevisao;
import com.aps.weather.demo.config.FusoHorario;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import com.aps.weather.demo.exception.DadoInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Leitura read-through das previsões: serve o snapshot persistido enquanto ele
 * estiver dentro do TTL; caso contrário consulta a fonte, substitui o snapshot e
 * responde com o que foi gravado. Se a fonte falhar e houver snapshot, mesmo
 * vencido, ele é servido — a ausência de dados novos é melhor que um erro.
 */
@Service
public class PrevisaoService {

    private static final int HORAS_DO_DIA = 24;
    private static final int DIAS_PREVISAO = 7;

    private final WeatherQueryService cidades;
    private final FonteDePrevisao fonte;
    private final PrevisaoStore store;
    private final FusoHorario fuso;
    private final Clock relogio;
    private final long ttlMs;

    // Uma trava por localização evita que requisições concorrentes consultem a
    // Open-Meteo ao mesmo tempo para a mesma cidade.
    private final Map<String, ReentrantLock> travas = new ConcurrentHashMap<>();

    public PrevisaoService(WeatherQueryService cidades,
                           FonteDePrevisao fonte,
                           PrevisaoStore store,
                           FusoHorario fuso,
                           Clock relogio,
                           @Value("${app.weather.forecast.ttl-ms:900000}") long ttlMs) {
        this.cidades = cidades;
        this.fonte = fonte;
        this.store = store;
        this.fuso = fuso;
        this.relogio = relogio;
        this.ttlMs = ttlMs;
    }

    public ResultadoPrevisaoHoraria horarias(String cidade) throws Exception {
        Localizacao local = cidades.buscarCidade(cidade);
        ZoneId zona = fuso.zona(local);
        LocalDate hoje = LocalDate.now(relogio.withZone(zona));
        Instant inicio = hoje.atStartOfDay(zona).toInstant();
        Instant fim = hoje.plusDays(1).atStartOfDay(zona).toInstant();

        List<PrevisaoHoraria> snapshot = store.horarias(local, inicio, fim);
        if (horarioAtual(snapshot)) {
            return new ResultadoPrevisaoHoraria(hoje, zona, snapshot);
        }

        ReentrantLock trava = trava(local);
        trava.lock();
        try {
            // Outra requisição pode ter atualizado enquanto esperávamos a trava.
            snapshot = store.horarias(local, inicio, fim);
            if (horarioAtual(snapshot)) {
                return new ResultadoPrevisaoHoraria(hoje, zona, snapshot);
            }
            try {
                store.substituirHorarias(local, fonte.buscarHorarias(local));
                List<PrevisaoHoraria> atuais = store.horarias(local, inicio, fim);
                if (!horarioCompleto(atuais)) {
                    throw new DadoInvalidoException(
                            "previsão horária incompleta para o dia atual: "
                                    + atuais.size() + " de " + HORAS_DO_DIA + " horas");
                }
                return new ResultadoPrevisaoHoraria(hoje, zona, atuais);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                if (horarioCompleto(snapshot)) {
                    return new ResultadoPrevisaoHoraria(hoje, zona, snapshot);
                }
                throw e;
            }
        } finally {
            trava.unlock();
        }
    }

    public ResultadoPrevisaoDiaria diarias(String cidade) throws Exception {
        Localizacao local = cidades.buscarCidade(cidade);
        ZoneId zona = fuso.zona(local);
        LocalDate hoje = LocalDate.now(relogio.withZone(zona));
        LocalDate ultimoDia = hoje.plusDays(DIAS_PREVISAO - 1);

        List<PrevisaoDiaria> snapshot = store.diarias(local, hoje, ultimoDia);
        if (diarioAtual(snapshot, hoje)) {
            return new ResultadoPrevisaoDiaria(zona, snapshot);
        }

        ReentrantLock trava = trava(local);
        trava.lock();
        try {
            snapshot = store.diarias(local, hoje, ultimoDia);
            if (diarioAtual(snapshot, hoje)) {
                return new ResultadoPrevisaoDiaria(zona, snapshot);
            }
            try {
                store.substituirDiarias(local, fonte.buscarDiarias(local));
                List<PrevisaoDiaria> atuais = store.diarias(local, hoje, ultimoDia);
                if (!diarioCompleto(atuais, hoje)) {
                    throw new DadoInvalidoException(
                            "previsão diária incompleta para o período: "
                                    + atuais.size() + " de " + DIAS_PREVISAO + " dias");
                }
                return new ResultadoPrevisaoDiaria(zona, atuais);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                if (diarioCompleto(snapshot, hoje)) {
                    return new ResultadoPrevisaoDiaria(zona, snapshot);
                }
                throw e;
            }
        } finally {
            trava.unlock();
        }
    }

    private ReentrantLock trava(Localizacao local) {
        return travas.computeIfAbsent(local.getCityNormalizada(), cidade -> new ReentrantLock());
    }

    private boolean horarioAtual(List<PrevisaoHoraria> previsoes) {
        return horarioCompleto(previsoes) && dentroDoTtl(previsoes.get(0).getUpdatedAt());
    }

    private boolean diarioAtual(List<PrevisaoDiaria> previsoes, LocalDate hoje) {
        return diarioCompleto(previsoes, hoje) && dentroDoTtl(previsoes.get(0).getUpdatedAt());
    }

    /**
     * Um snapshot só é utilizável quando cobre exatamente as 24 horas do dia: um
     * conjunto parcial alimentaria o gráfico com uma curva truncada.
     */
    private static boolean horarioCompleto(List<PrevisaoHoraria> previsoes) {
        return previsoes.size() == HORAS_DO_DIA;
    }

    /**
     * Exige os sete dias consecutivos a partir de hoje, não apenas as datas das
     * pontas — lacunas no meio também tornam o snapshot incompleto.
     */
    private static boolean diarioCompleto(List<PrevisaoDiaria> previsoes, LocalDate hoje) {
        if (previsoes.size() != DIAS_PREVISAO) {
            return false;
        }
        for (int dia = 0; dia < DIAS_PREVISAO; dia++) {
            if (!previsoes.get(dia).getForecastDate().equals(hoje.plusDays(dia))) {
                return false;
            }
        }
        return true;
    }

    private boolean dentroDoTtl(Instant atualizadoEm) {
        return atualizadoEm != null
                && atualizadoEm.plusMillis(ttlMs).isAfter(Instant.now(relogio));
    }
}
