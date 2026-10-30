package com.aps.weather.demo.api;

import com.aps.weather.demo.service.ApiException;
import com.aps.weather.demo.service.CidadeNaoEncontradaException;
import com.aps.weather.demo.service.DadoInvalidoException;
import com.aps.weather.demo.service.RequisicaoInvalidaException;
import com.aps.weather.demo.service.SemDadosClimaticosException;
import tools.jackson.core.JacksonException;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Resposta pública para falhas de infraestrutura: nunca repete a mensagem interna. */
    private static final String DETALHE_BANCO =
            "Não foi possível concluir a operação no banco de dados";

    @ExceptionHandler(CidadeNaoEncontradaException.class)
    public ProblemDetail cidadeNaoEncontrada(CidadeNaoEncontradaException e) {
        return problema(HttpStatus.NOT_FOUND, "Cidade não encontrada", e.getMessage());
    }

    @ExceptionHandler({RequisicaoInvalidaException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail requisicaoInvalida(Exception e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage());
    }

    @ExceptionHandler(SemDadosClimaticosException.class)
    public ProblemDetail semDados(SemDadosClimaticosException e) {
        return problema(HttpStatus.NOT_FOUND, "Sem dados climáticos", e.getMessage());
    }

    @ExceptionHandler({DadoInvalidoException.class, JacksonException.class})
    public ProblemDetail dadoInvalido(Exception e) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Dados inválidos", e.getMessage());
    }

    @ExceptionHandler({ApiException.class, ConnectException.class, HttpTimeoutException.class})
    public ProblemDetail falhaNaFonte(Exception e) {
        return problema(HttpStatus.BAD_GATEWAY,
                "Falha ao consultar a fonte de clima", e.getMessage());
    }

    @ExceptionHandler(InterruptedException.class)
    public ProblemDetail interrompido(InterruptedException e) {
        Thread.currentThread().interrupt();
        return problema(HttpStatus.SERVICE_UNAVAILABLE,
                "Coleta interrompida", e.getMessage());
    }

    /**
     * Mensagens de persistência trazem SQL, nome de constraint e outros detalhes
     * internos. Elas ficam no log; o cliente recebe um texto fixo.
     */
    @ExceptionHandler({PersistenceException.class, DataAccessException.class})
    public ProblemDetail falhaNoBanco(Exception e) {
        LOG.error("Falha de banco de dados", e);
        return problema(HttpStatus.SERVICE_UNAVAILABLE,
                "Erro no banco de dados", DETALHE_BANCO);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
