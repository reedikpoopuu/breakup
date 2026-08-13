package com.example.demo.supplier;

import com.example.demo.common.CountryCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "suppliers", uniqueConstraints = @UniqueConstraint(columnNames = {"country", "name"}))
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CountryCode country;

    @Column(nullable = false)
    private String name;

    @Column(name = "rfq_email", nullable = false)
    private String rfqEmail;

    @Column(name = "price_url", nullable = false)
    private String priceUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Supplier() {
        // JPA
    }

    public Supplier(CountryCode country, String name, String rfqEmail, String priceUrl) {
        this.country = country;
        this.name = name;
        this.rfqEmail = rfqEmail;
        this.priceUrl = priceUrl;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void update(String name, String rfqEmail, String priceUrl) {
        this.name = name;
        this.rfqEmail = rfqEmail;
        this.priceUrl = priceUrl;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public CountryCode getCountry() {
        return country;
    }

    public String getName() {
        return name;
    }

    public String getRfqEmail() {
        return rfqEmail;
    }

    public String getPriceUrl() {
        return priceUrl;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
