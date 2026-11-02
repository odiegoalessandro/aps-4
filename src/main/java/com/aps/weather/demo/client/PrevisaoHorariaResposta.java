package com.aps.weather.demo.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrevisaoHorariaResposta {
    public String timezone;
    public Horaria hourly;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Horaria {
        public List<String> time;

        @JsonProperty("temperature_2m")
        public List<Double> temperature2m;
    }
}
