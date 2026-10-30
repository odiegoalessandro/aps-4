package com.aps.weather.demo.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrevisaoResposta {
    public double latitude;
    public double longitude;
    public String timezone;
    public CondicaoAtual current;
}
