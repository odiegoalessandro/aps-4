package com.aps.weather.demo.exception;

public class ApiException extends Exception {
    private final int status;
    private final String corpo;

    public ApiException(int status, String corpo) {
        super("API respondeu com status " + status);
        this.status = status;
        this.corpo = corpo;
    }

    public int getStatus() { return status; }
    public String getCorpo() { return corpo; }
}
