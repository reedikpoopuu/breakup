package com.example.demo.audit;

import com.example.demo.common.CountryCode;

import java.time.Instant;

public record AuditLogEntryResponse(
        Long id,
        Instant occurredAt,
        AuditActionType actionType,
        String actorSmartIdIdentity,
        String actorDisplayName,
        CountryCode country,
        String requestDetail,
        String responseSummary,
        boolean successful,
        String errorMessage
) {
    public static AuditLogEntryResponse from(AuditLogEntry entry) {
        return new AuditLogEntryResponse(
                entry.getId(),
                entry.getOccurredAt(),
                entry.getActionType(),
                entry.getActorSmartIdIdentity(),
                entry.getActorDisplayName(),
                entry.getCountry(),
                entry.getRequestDetail(),
                entry.getResponseSummary(),
                entry.isSuccessful(),
                entry.getErrorMessage()
        );
    }
}
