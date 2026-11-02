package com.aps.weather.demo.config;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.exception.DadoInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Resolve a timezone efetiva de uma localização. O campo da localização é a
 * fonte de verdade; o padrão do Brasil só entra quando ele está vazio.
 */
@Component
public class FusoHorario {

    private static final String PADRAO_BRASIL = "America/Sao_Paulo";

    private final String padrao;

    public FusoHorario(
            @Value("${app.weather.timezone.default:" + PADRAO_BRASIL + "}") String padrao) {
        this.padrao = (padrao == null || padrao.isBlank()) ? PADRAO_BRASIL : padrao.trim();
    }

    public ZoneId zona(Localizacao local) throws DadoInvalidoException {
        String timezone = local.getTimezone();
        if (timezone == null || timezone.isBlank()) {
            timezone = padrao;
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException e) {
            throw new DadoInvalidoException(
                    "timezone inválida para " + local.getCity() + ": " + timezone);
        }
    }

    public String id(Localizacao local) throws DadoInvalidoException {
        return zona(local).getId();
    }
}
