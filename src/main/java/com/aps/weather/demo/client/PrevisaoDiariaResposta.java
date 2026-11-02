package com.aps.weather.demo.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrevisaoDiariaResposta {
    public String timezone;
    public Diaria daily;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Diaria {
        public List<String> time;

        @JsonProperty("temperature_2m_min")
        public List<Double> temperature2mMin;

        @JsonProperty("temperature_2m_max")
        public List<Double> temperature2mMax;
    }
}
