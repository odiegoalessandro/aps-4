package com.aps.weather.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_forecasts")
public class PrevisaoDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Localizacao location;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "min_temperature", nullable = false)
    private double minTemperature;

    @Column(name = "max_temperature", nullable = false)
    private double maxTemperature;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PrevisaoDiaria() {}   // exigido pelo Hibernate

    public PrevisaoDiaria(Localizacao location, LocalDate forecastDate, double minTemperature,
                          double maxTemperature, Instant updatedAt) {
        this.location = location;
        this.forecastDate = forecastDate;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Localizacao getLocation() { return location; }
    public LocalDate getForecastDate() { return forecastDate; }
    public double getMinTemperature() { return minTemperature; }
    public double getMaxTemperature() { return maxTemperature; }
    public Instant getUpdatedAt() { return updatedAt; }
}
