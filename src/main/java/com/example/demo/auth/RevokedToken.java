package com.example.demo.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One denylisted token, keyed by its {@code jti} (a random ID minted per-issuance in
 * {@link TokenService#issue}, not derived from the user - two tokens for the same user
 * never collide). {@code expiresAt} mirrors the token's own expiry; nothing currently
 * prunes rows once that passes (would be a small {@code @Scheduled} job - the app has no
 * scheduling infrastructure yet, and row growth is bounded by login/logout churn, so
 * this is left as a known follow-up rather than built speculatively here).
 */
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    private String jti;

    @Column(nullable = false)
    private Instant revokedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    protected RevokedToken() {
        // JPA
    }

    public RevokedToken(String jti, Instant revokedAt, Instant expiresAt) {
        this.jti = jti;
        this.revokedAt = revokedAt;
        this.expiresAt = expiresAt;
    }

    public String getJti() {
        return jti;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
