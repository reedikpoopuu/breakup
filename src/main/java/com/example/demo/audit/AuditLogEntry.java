package com.example.demo.audit;

import com.example.demo.common.CountryCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One customer-triggered action against a system that reaches outside this app on the
 * customer's behalf (contract parsing today; DataHub consumption fetches and business
 * registry lookups once those exist for real - see {@link AuditActionType}). Written
 * once, never updated - this is meant to stand as durable proof of what happened, not a
 * live/mutable record. Both successful and failed attempts are recorded: a rejected
 * upload is still a customer action worth having proof of.
 */
@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditActionType actionType;

    @Column(nullable = false)
    private String actorSmartIdIdentity;

    private String actorDisplayName;

    @Enumerated(EnumType.STRING)
    private CountryCode country;

    @Column(length = 500)
    private String requestDetail;

    /** What the system returned to the customer - e.g. the full extracted-fields JSON - kept as the actual proof of what was disclosed, not a vague summary. */
    @Lob
    private String responseSummary;

    @Column(nullable = false)
    private boolean successful;

    @Column(length = 1000)
    private String errorMessage;

    protected AuditLogEntry() {
        // JPA
    }

    public AuditLogEntry(Instant occurredAt, AuditActionType actionType, String actorSmartIdIdentity,
                          String actorDisplayName, CountryCode country, String requestDetail,
                          String responseSummary, boolean successful, String errorMessage) {
        this.occurredAt = occurredAt;
        this.actionType = actionType;
        this.actorSmartIdIdentity = actorSmartIdIdentity;
        this.actorDisplayName = actorDisplayName;
        this.country = country;
        this.requestDetail = requestDetail;
        this.responseSummary = responseSummary;
        this.successful = successful;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public String getActorSmartIdIdentity() {
        return actorSmartIdIdentity;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public CountryCode getCountry() {
        return country;
    }

    public String getRequestDetail() {
        return requestDetail;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
