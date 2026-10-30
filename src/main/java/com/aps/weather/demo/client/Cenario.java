package com.aps.weather.demo.client;

public enum Cenario {
    //               temp.  umid.  chuva  vento  pressão  nuvens  código WMO
    DIA_CALMO(        24.0,  55,    0.0,   8.0,  1015.0,   15.0,  0),
    ONDA_DE_CALOR(    38.0,  25,    0.0,   6.0,  1008.0,    5.0,  0),
    FRENTE_FRIA(       9.0,  80,    2.0,  30.0,  1022.0,   85.0,  61),
    TEMPESTADE(       19.0,  95,   12.0,  55.0,   995.0,  100.0,  95);

    public final double temperatura;
    public final int umidade;
    public final double precipitacao;
    public final double vento;
    public final double pressao;
    public final double nuvens;
    public final int codigoTempo;

    Cenario(double temperatura, int umidade, double precipitacao, double vento,
            double pressao, double nuvens, int codigoTempo) {
        this.temperatura = temperatura;
        this.umidade = umidade;
        this.precipitacao = precipitacao;
        this.vento = vento;
        this.pressao = pressao;
        this.nuvens = nuvens;
        this.codigoTempo = codigoTempo;
    }

    public static Cenario deTexto(String texto) {
        try {
            return valueOf(texto.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cenário desconhecido: " + texto
                    + ". Opções: DIA_CALMO, ONDA_DE_CALOR, FRENTE_FRIA, TEMPESTADE");
        }
    }
}
