package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Nord Pool day-ahead spot price, shared reference data across all three countries. */
@Entity
@Table(name = "spot_prices")
public class SpotPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CountryCode country;

    @Column(name = "interval_start", nullable = false)
    private Instant intervalStart;

    @Column(name = "price_eur_per_mwh", nullable = false, precision = 19, scale = 4)
    private BigDecimal priceEurPerMwh;

    protected SpotPrice() {
        // JPA
    }

    public SpotPrice(CountryCode country, Instant intervalStart, BigDecimal priceEurPerMwh) {
        this.country = country;
        this.intervalStart = intervalStart;
        this.priceEurPerMwh = priceEurPerMwh;
    }

    public Long getId() {
        return id;
    }

    public CountryCode getCountry() {
        return country;
    }

    public Instant getIntervalStart() {
        return intervalStart;
    }

    public BigDecimal getPriceEurPerMwh() {
        return priceEurPerMwh;
    }
}
