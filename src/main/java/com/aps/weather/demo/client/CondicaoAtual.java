package com.aps.weather.demo.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CondicaoAtual {
    public String time;

    @JsonProperty("temperature_2m")
    public Double temperatura;

    @JsonProperty("apparent_temperature")
    public Double sensacaoTermica;

    @JsonProperty("relative_humidity_2m")
    public Integer umidade;

    @JsonProperty("precipitation")
    public Double precipitacao;

    @JsonProperty("weather_code")
    public Integer codigoTempo;

    @JsonProperty("wind_speed_10m")
    public Double vento;

    @JsonProperty("wind_direction_10m")
    public Double direcaoVento;

    @JsonProperty("pressure_msl")
    public Double pressao;

    @JsonProperty("cloud_cover")
    public Double nuvens;
}
