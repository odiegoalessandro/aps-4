package com.aps.weather.demo.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Forma canônica usada para comparar e garantir unicidade de cidades.
 *
 * O SQLite só sabe comparar sem caixa em ASCII: {@code COLLATE NOCASE} e
 * {@code lower()} deixam "SÃO PAULO" distinto de "São Paulo". Por isso a
 * comparação é feita sobre uma coluna já normalizada em Java, que remove
 * acentos ({@code São Paulo} → {@code sao paulo}) e ignora caixa.
 */
public final class NomeDeCidade {

    private NomeDeCidade() {}

    public static String normalizar(String cidade) {
        String semAcento = Normalizer.normalize(cidade.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}
