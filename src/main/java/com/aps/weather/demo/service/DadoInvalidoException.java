package com.aps.weather.demo.service;

public class DadoInvalidoException extends Exception {
    public DadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
