package com.aps.weather.demo.service;

/** Falha de coleta de uma cidade, com o motivo legível já calculado. */
public record FalhaColeta(String cidade, String motivo) {
}
