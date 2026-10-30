package com.aps.weather.demo.domain;

public enum CondicaoClimatica {
    CEU_LIMPO(0, "Céu limpo"),
    PREDOMINANTEMENTE_LIMPO(1, "Predominantemente limpo"),
    PARCIALMENTE_NUBLADO(2, "Parcialmente nublado"),
    NUBLADO(3, "Nublado"),

    NEBLINA(45, "Neblina"),
    NEBLINA_COM_GEADA(48, "Neblina com geada"),

    GAROA_FRACA(51, "Garoa fraca"),
    GAROA_MODERADA(53, "Garoa moderada"),
    GAROA_INTENSA(55, "Garoa intensa"),
    GAROA_CONGELANTE_FRACA(56, "Garoa congelante fraca"),
    GAROA_CONGELANTE_INTENSA(57, "Garoa congelante intensa"),

    CHUVA_FRACA(61, "Chuva fraca"),
    CHUVA_MODERADA(63, "Chuva moderada"),
    CHUVA_FORTE(65, "Chuva forte"),
    CHUVA_CONGELANTE_FRACA(66, "Chuva congelante fraca"),
    CHUVA_CONGELANTE_FORTE(67, "Chuva congelante forte"),

    NEVE_FRACA(71, "Neve fraca"),
    NEVE_MODERADA(73, "Neve moderada"),
    NEVE_FORTE(75, "Neve forte"),
    GRAOS_DE_NEVE(77, "Grãos de neve"),

    PANCADAS_DE_CHUVA_FRACAS(80, "Pancadas de chuva fracas"),
    PANCADAS_DE_CHUVA_MODERADAS(81, "Pancadas de chuva moderadas"),
    PANCADAS_DE_CHUVA_VIOLENTAS(82, "Pancadas de chuva violentas"),
    PANCADAS_DE_NEVE_FRACAS(85, "Pancadas de neve fracas"),
    PANCADAS_DE_NEVE_FORTES(86, "Pancadas de neve fortes"),

    TEMPESTADE(95, "Tempestade"),
    TEMPESTADE_COM_GRANIZO_FRACO(96, "Tempestade com granizo fraco"),
    TEMPESTADE_FORTE(97, "Tempestade forte"),
    TEMPESTADE_COM_GRANIZO_FORTE(99, "Tempestade com granizo forte"),

    DESCONHECIDO(-1, "Desconhecido");

    private final int codigo;
    private final String descricao;

    CondicaoClimatica(int codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public static CondicaoClimatica deCodigo(int codigo) {
        for (CondicaoClimatica c : values()) {
            if (c.codigo == codigo) {
                return c;
            }
        }
        return DESCONHECIDO;
    }
}
