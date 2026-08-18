package com.example.demo.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A Smart-ID authenticated identity. Smart-ID authenticates a person by national
 * identity, not name or email, so {@code smartIdIdentity} (semantics identifier /
 * national ID number + country) - not display name - is the login key. See
 * ARCH_SPEC.md section 2.2.
 */
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "smart_id_identity", nullable = false, unique = true)
    private String smartIdIdentity;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppUser() {
        // JPA
    }

    public AppUser(String smartIdIdentity, String displayName, Role role) {
        this.smartIdIdentity = smartIdIdentity;
        this.displayName = displayName;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    void promoteToAdmin() {
        this.role = Role.ADMIN;
    }

    void demoteFromAdmin() {
        this.role = Role.USER;
    }

    public Long getId() {
        return id;
    }

    public String getSmartIdIdentity() {
        return smartIdIdentity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
