package com.aps.weather.demo.config;

import com.aps.weather.demo.service.ColetaService;
import com.aps.weather.demo.service.ResultadoColeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.weather.collection.enabled", havingValue = "true", matchIfMissing = true)
public class ColetaAgendada {

    private static final Logger LOG = LoggerFactory.getLogger(ColetaAgendada.class);

    private final ColetaService coletaService;

    public ColetaAgendada(ColetaService coletaService) {
        this.coletaService = coletaService;
    }

    @Scheduled(
            fixedDelayString = "${app.weather.collection.interval-ms:900000}",
            initialDelayString = "${app.weather.collection.initial-delay-ms:5000}")
    public void coletar() {
        try {
            ResultadoColeta resultado = coletaService.coletarTodas();
            LOG.info("Coleta agendada concluída: {} previsão(ões) nova(s) gravada(s), {} falha(s)",
                    resultado.gravadas().size(), resultado.falhas().size());
        } catch (Exception e) {
            LOG.error("Erro inesperado na coleta agendada", e);
        }
    }
}
