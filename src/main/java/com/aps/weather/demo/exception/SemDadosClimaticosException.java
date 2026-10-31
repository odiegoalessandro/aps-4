package com.aps.weather.demo.exception;

public class SemDadosClimaticosException extends RuntimeException {

    public SemDadosClimaticosException(String cidade) {
        super("Ainda não há coleta registrada para a cidade: " + cidade);
    }
}
