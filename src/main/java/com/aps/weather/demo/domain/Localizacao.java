package com.aps.weather.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "locations")
public class Localizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String city;

    /**
     * Espelho de {@code city} na forma de {@link NomeDeCidade#normalizar(String)}.
     * É a coluna comparada nas buscas e protegida pelo índice único — o
     * {@code COLLATE NOCASE} do SQLite não ignora acentos.
     */
    @Column(name = "city_normalizada", nullable = false, length = 150, updatable = false)
    private String cityNormalizada;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false, length = 100)
    private String timezone;

    protected Localizacao() {}   // exigido pelo Hibernate

    public Localizacao(String city, double latitude, double longitude, String timezone) {
        this.city = city;
        this.cityNormalizada = NomeDeCidade.normalizar(city);
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
    }

    public Long getId() { return id; }
    public String getCity() { return city; }
    public String getCityNormalizada() { return cityNormalizada; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getTimezone() { return timezone; }
}
