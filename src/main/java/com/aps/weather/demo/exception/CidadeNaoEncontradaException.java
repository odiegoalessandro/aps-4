package com.aps.weather.demo.exception;

public class CidadeNaoEncontradaException extends RuntimeException {

    public CidadeNaoEncontradaException(String cidade) {
        super("Cidade não encontrada: " + cidade);
    }
}
