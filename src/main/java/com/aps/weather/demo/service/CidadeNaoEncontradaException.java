package com.aps.weather.demo.service;

public class CidadeNaoEncontradaException extends RuntimeException {

    public CidadeNaoEncontradaException(String cidade) {
        super("Cidade não encontrada: " + cidade);
    }
}
