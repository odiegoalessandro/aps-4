package com.aps.weather.demo.client;

import com.aps.weather.demo.config.FusoHorario;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.domain.PrevisaoHoraria;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@ConditionalOnProperty(name = "app.weather.source", havingValue = "simulada")
public class ClienteSimulado implements FonteDeClima, FonteDePrevisao {

    private static final int HORAS_DO_DIA = 24;
    private static final int DIAS_DE_PREVISAO = 7;

    private final Cenario cenario;
    private final FusoHorario fuso;
    private final Clock relogio;
    private final Random random = new Random();

    public ClienteSimulado(
            @Value("${app.weather.simulation.scenario:DIA_CALMO}") String cenario,
            FusoHorario fuso,
            Clock relogio) {
        this.cenario = Cenario.deTexto(cenario);
        this.fuso = fuso;
        this.relogio = relogio;
    }

    @Override
    public PrevisaoDoCLima buscar(Localizacao local) {
        // cada coleta tem horário próprio, então nunca é "repetida" no simulador
        Instant agora = Instant.now(relogio).truncatedTo(ChronoUnit.SECONDS);

        // onda lenta (ciclo de uns 6 minutos), com uma fase diferente para cada cidade
        double fase = fase(local);
        double onda = Math.sin(agora.getEpochSecond() / 60.0 + fase);

        double temperatura = cenario.temperatura + 3 * onda + ruido(0.3);
        int umidade = (int) limitar(cenario.umidade + 8 * onda + ruido(1), 0, 100);
        double precipitacao = cenario.precipitacao == 0
                ? 0
                : Math.max(0, cenario.precipitacao * (1 + 0.5 * onda) + ruido(0.3));
        double vento = Math.max(0, cenario.vento * (1 + 0.3 * onda) + ruido(1));
        double direcao = limitar(200 + 80 * onda + ruido(5), 0, 360);
        double pressao = cenario.pressao + 2 * onda + ruido(0.2);
        double nuvens = limitar(cenario.nuvens + 10 * onda + ruido(2), 0, 100);
        double sensacao = temperatura - vento * 0.08 + (umidade - 50) * 0.03;

        return new PrevisaoDoCLima(
                local,
                agora,
                arredondar(temperatura),
                arredondar(sensacao),
                umidade,
                arredondar(precipitacao),
                cenario.codigoTempo,
                arredondar(vento),
                arredondar(direcao),
                arredondar(pressao),
                arredondar(nuvens),
                Instant.now(relogio));
    }

    @Override
    public List<PrevisaoHoraria> buscarHorarias(Localizacao local) throws Exception {
        ValidadorDeLocalizacao.validarCoordenadas(local);

        ZoneId zona = fuso.zona(local);
        LocalDate hoje = LocalDate.now(relogio.withZone(zona));
        Instant inicioDoDia = hoje.atStartOfDay(zona).toInstant();
        Instant atualizadoEm = Instant.now(relogio);

        List<PrevisaoHoraria> previsoes = new ArrayList<>(HORAS_DO_DIA);
        for (int hora = 0; hora < HORAS_DO_DIA; hora++) {
            // Somar horas ao instante (e não ao horário local) mantém os instantes
            // únicos mesmo em fusos com mudança de horário de verão.
            Instant momento = inicioDoDia.plus(hora, ChronoUnit.HOURS);
            double temperatura = cenario.temperatura
                    + 5 * Math.sin((hora - 9) * Math.PI / 12)
                    + ruido(0.3);
            previsoes.add(new PrevisaoHoraria(
                    local, momento, arredondar(temperatura), atualizadoEm));
        }
        return previsoes;
    }

    @Override
    public List<PrevisaoDiaria> buscarDiarias(Localizacao local) throws Exception {
        ValidadorDeLocalizacao.validarCoordenadas(local);

        ZoneId zona = fuso.zona(local);
        LocalDate hoje = LocalDate.now(relogio.withZone(zona));
        Instant atualizadoEm = Instant.now(relogio);

        List<PrevisaoDiaria> previsoes = new ArrayList<>(DIAS_DE_PREVISAO);
        for (int dia = 0; dia < DIAS_DE_PREVISAO; dia++) {
            double base = cenario.temperatura
                    + 2 * Math.sin((hoje.getDayOfYear() + dia) * Math.PI / 3)
                    + ruido(0.3);
            double minima = base - 4 - Math.abs(ruido(0.5));
            double maxima = base + 4 + Math.abs(ruido(0.5));
            previsoes.add(new PrevisaoDiaria(
                    local, hoje.plusDays(dia), arredondar(minima), arredondar(maxima),
                    atualizadoEm));
        }
        return previsoes;
    }

    private static double fase(Localizacao local) {
        return Math.abs(local.getCity().hashCode() % 1000) / 1000.0 * 2 * Math.PI;
    }

    private double ruido(double desvio) {
        return random.nextGaussian() * desvio;
    }

    private static double limitar(double valor, double min, double max) {
        return Math.max(min, Math.min(max, valor));
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
