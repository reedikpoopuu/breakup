package com.example.demo.audit;

/**
 * Categories of customer-triggered actions this app keeps an audit trail of, so admins
 * can produce proof of what a customer asked the system to do (and what it returned) if
 * an authority asks. {@code CONTRACT_PARSE} is wired up today ({@code
 * ContractUploadController}); {@code DATAHUB_CONSUMPTION_FETCH} and {@code
 * BUSINESS_REGISTRY_LOOKUP} exist so the moment those features have a real,
 * customer-triggered endpoint (neither does yet - see each package's javadoc), logging
 * is a one-line call into {@link AuditLogService}, not a new subsystem.
 */
public enum AuditActionType {
    CONTRACT_PARSE,
    DATAHUB_CONSUMPTION_FETCH,
    BUSINESS_REGISTRY_LOOKUP
}
