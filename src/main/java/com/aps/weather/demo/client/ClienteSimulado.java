package com.aps.weather.demo.client;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Component
@ConditionalOnProperty(name = "app.weather.source", havingValue = "simulada")
public class ClienteSimulado implements FonteDeClima {

    private final Cenario cenario;
    private final Random random = new Random();

    public ClienteSimulado(
            @Value("${app.weather.simulation.scenario:DIA_CALMO}") String cenario) {
        this.cenario = Cenario.deTexto(cenario);
    }

    @Override
    public PrevisaoDoCLima buscar(Localizacao local) {
        // cada coleta tem horário próprio, então nunca é "repetida" no simulador
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // onda lenta (ciclo de uns 6 minutos), com uma fase diferente para cada cidade
        double fase = Math.abs(local.getCity().hashCode() % 1000) / 1000.0 * 2 * Math.PI;
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
                Instant.now());
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
