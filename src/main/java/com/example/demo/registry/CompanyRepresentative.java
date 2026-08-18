package com.example.demo.registry;

import java.time.LocalDate;

/**
 * One person with a registered right of representation over a company, as returned by
 * a national business registry (ARCHITECTURE.md section 3.2). {@code personalIdCode} +
 * {@code personalIdCountry} together are the login-identity match key - see
 * {@link RepresentativeRightsVerificationService} - not {@code givenName}/{@code
 * surname}, which exist for audit/display only and are never compared against a login.
 */
public record CompanyRepresentative(
        String givenName,
        String surname,
        String personalIdCode,
        String personalIdCountry,
        LocalDate dateOfBirth,
        String role,
        String roleText,
        boolean exclusiveRightOfRepresentation
) {
}
