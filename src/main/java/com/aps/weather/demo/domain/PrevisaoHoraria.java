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

@Entity
@Table(name = "hourly_forecasts")
public class PrevisaoHoraria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Localizacao location;

    @Column(name = "forecast_at", nullable = false)
    private Instant forecastAt;

    @Column(nullable = false)
    private double temperature;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PrevisaoHoraria() {}   // exigido pelo Hibernate

    public PrevisaoHoraria(Localizacao location, Instant forecastAt, double temperature,
                           Instant updatedAt) {
        this.location = location;
        this.forecastAt = forecastAt;
        this.temperature = temperature;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Localizacao getLocation() { return location; }
    public Instant getForecastAt() { return forecastAt; }
    public double getTemperature() { return temperature; }
    public Instant getUpdatedAt() { return updatedAt; }
}
