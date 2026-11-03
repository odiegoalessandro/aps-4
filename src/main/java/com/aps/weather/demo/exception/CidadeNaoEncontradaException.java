package com.aps.weather.demo.exception;

public class CidadeNaoEncontradaException extends RuntimeException {

    public CidadeNaoEncontradaException(String cidade) {
        super("Cidade não encontrada: " + cidade);
    }

    private CidadeNaoEncontradaException(Long id) {
        super("Cidade não encontrada para o id " + id);
    }

    public static CidadeNaoEncontradaException paraId(Long id) {
        return new CidadeNaoEncontradaException(id);
    }
}
