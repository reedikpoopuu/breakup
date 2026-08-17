package com.example.demo.audit;

import com.example.demo.auth.TokenService;
import com.example.demo.common.CountryCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Writes {@link AuditLogEntry} rows. A logging failure here must never break the
 * customer-facing request that triggered it - every write is caught and logged at ERROR
 * rather than propagated, since losing an audit record is bad but failing a real
 * customer action because audit persistence hiccupped would be worse.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(AuditActionType actionType, TokenService.Principal actor, CountryCode country,
                        String requestDetail, String responseSummary, boolean successful, String errorMessage) {
        try {
            AuditLogEntry entry = new AuditLogEntry(
                    Instant.now(), actionType,
                    actor != null ? actor.smartIdIdentity() : "unknown",
                    actor != null ? actor.displayName() : "unknown",
                    country, requestDetail, responseSummary, successful, errorMessage);
            repository.save(entry);
        } catch (RuntimeException e) {
            log.error("Failed to persist audit log entry for {} by {}", actionType,
                    actor != null ? actor.smartIdIdentity() : "unknown", e);
        }
    }

    /** Convenience for callers that have a response object rather than an already-serialized string. */
    public void recordWithJsonResponse(AuditActionType actionType, TokenService.Principal actor, CountryCode country,
                                        String requestDetail, Object response, boolean successful, String errorMessage) {
        String responseSummary = null;
        if (response != null) {
            try {
                responseSummary = objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException e) {
                responseSummary = String.valueOf(response);
            }
        }
        record(actionType, actor, country, requestDetail, responseSummary, successful, errorMessage);
    }
}
