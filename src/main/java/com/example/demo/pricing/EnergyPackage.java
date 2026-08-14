package com.example.demo.pricing;

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

/** A default energy offer as last observed by the price scraper - admin-visible only. */
@Entity
@Table(name = "energy_packages")
public class EnergyPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CountryCode country;

    @Column(name = "price_per_kwh", nullable = false, precision = 10, scale = 4)
    private BigDecimal pricePerKwh;

    @Column(name = "margin_per_kwh", nullable = false, precision = 10, scale = 4)
    private BigDecimal marginPerKwh;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    protected EnergyPackage() {
        // JPA
    }

    public EnergyPackage(String packageName, String supplierName, CountryCode country,
                          BigDecimal pricePerKwh, BigDecimal marginPerKwh) {
        this.packageName = packageName;
        this.supplierName = supplierName;
        this.country = country;
        this.pricePerKwh = pricePerKwh;
        this.marginPerKwh = marginPerKwh;
        this.lastUpdated = Instant.now();
    }

    public void applyScrapedPrice(BigDecimal pricePerKwh, BigDecimal marginPerKwh) {
        this.pricePerKwh = pricePerKwh;
        this.marginPerKwh = marginPerKwh;
        this.lastUpdated = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public CountryCode getCountry() {
        return country;
    }

    public BigDecimal getPricePerKwh() {
        return pricePerKwh;
    }

    public BigDecimal getMarginPerKwh() {
        return marginPerKwh;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }
}
