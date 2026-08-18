package com.example.demo.registry;

import java.time.Instant;

/**
 * Result of checking whether the authenticated user's personal ID code appears in a
 * company's registry-reported list of people with a right of representation
 * (ARCHITECTURE.md section 3.2 - "replaces a manual power-of-attorney step"). Callers
 * should store this alongside {@code checkedAt} as the audit trail for the legal claim
 * "this user may act for this company", not just trust it once and forget it (see
 * {@code AuditActionType.BUSINESS_REGISTRY_LOOKUP}).
 */
public record RepresentationRightsVerification(
        boolean verified,
        String registryCode,
        String personalIdCode,
        String personalIdCountry,
        String matchedRole,
        String matchedRoleText,
        boolean exclusiveRightOfRepresentation,
        Instant checkedAt
) {

    public static RepresentationRightsVerification notVerified(String registryCode, String personalIdCode, String personalIdCountry) {
        return new RepresentationRightsVerification(
                false, registryCode, personalIdCode, personalIdCountry, null, null, false, Instant.now());
    }

    public static RepresentationRightsVerification verified(String registryCode, CompanyRepresentative match) {
        return new RepresentationRightsVerification(
                true, registryCode, match.personalIdCode(), match.personalIdCountry(),
                match.role(), match.roleText(), match.exclusiveRightOfRepresentation(), Instant.now());
    }
}
