package com.example.demo.registry;

import com.example.demo.audit.AuditActionType;
import com.example.demo.audit.AuditLogService;
import com.example.demo.auth.TokenService;
import com.example.demo.common.CountryCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Any authenticated user can verify their own right to represent a company - this
 * replaces a manual power-of-attorney step (ARCHITECTURE.md section 3.2), so it is a
 * customer self-service check, not admin-only. Matches the {@code POST
 * /companies/:id/representative-rights/verify} sketch in ARCHITECTURE.md section 5;
 * {@code registryCode} stands in for {@code :id} until a persisted {@code Company}
 * entity exists to carry one. Every attempt is audited via {@code
 * AuditActionType.BUSINESS_REGISTRY_LOOKUP} - a positive result here is the legal basis
 * for letting this user act on the company's behalf, so it must be provable later.
 */
@RestController
@RequestMapping("/api/companies")
public class RepresentationRightsController {

    private final RepresentativeRightsVerificationService verificationService;
    private final AuditLogService auditLogService;

    public RepresentationRightsController(RepresentativeRightsVerificationService verificationService,
                                           AuditLogService auditLogService) {
        this.verificationService = verificationService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/{registryCode}/representative-rights/verify")
    public RepresentationRightsVerification verify(@PathVariable String registryCode,
                                                     @RequestParam CountryCode country,
                                                     @AuthenticationPrincipal TokenService.Principal actor) {
        String requestDetail = country + " " + registryCode;
        try {
            RepresentationRightsVerification result = verificationService.verify(country, registryCode, actor.smartIdIdentity());
            auditLogService.recordWithJsonResponse(AuditActionType.BUSINESS_REGISTRY_LOOKUP, actor, country,
                    requestDetail, result, true, null);
            return result;
        } catch (RuntimeException e) {
            auditLogService.recordWithJsonResponse(AuditActionType.BUSINESS_REGISTRY_LOOKUP, actor, country,
                    requestDetail, null, false, e.getMessage());
            throw e;
        }
    }
}
