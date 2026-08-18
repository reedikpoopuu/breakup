package com.example.demo.contract;

import com.example.demo.audit.AuditActionType;
import com.example.demo.audit.AuditLogService;
import com.example.demo.auth.TokenService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Any authenticated user can upload their own contract PDF (not admin-only - this is a
 * customer self-service step). Nothing is persisted here beyond the audit trail (see
 * {@link AuditLogService}, {@code AdminAuditLogController}): extraction is synchronous
 * and the result is handed back for the caller to confirm before anything is saved.
 * Every attempt - successful or not - is recorded, since a rejected upload is still a
 * customer action worth having proof of.
 */
@RestController
@RequestMapping("/api/contracts")
public class ContractUploadController {

    private final ContractPdfTextExtractor textExtractor;
    private final ContractFieldExtractor fieldExtractor;
    private final ContractPricingAiExtractor pricingAiExtractor;
    private final AuditLogService auditLogService;
    private final ContractParseRateLimiter rateLimiter;

    public ContractUploadController(ContractPdfTextExtractor textExtractor, ContractFieldExtractor fieldExtractor,
                                     ContractPricingAiExtractor pricingAiExtractor, AuditLogService auditLogService,
                                     ContractParseRateLimiter rateLimiter) {
        this.textExtractor = textExtractor;
        this.fieldExtractor = fieldExtractor;
        this.pricingAiExtractor = pricingAiExtractor;
        this.auditLogService = auditLogService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/parse")
    public ExtractedContractFields parse(@RequestParam("file") MultipartFile file,
                                          @AuthenticationPrincipal TokenService.Principal actor) {
        String rateLimitKey = actor != null ? actor.smartIdIdentity() : "unknown";
        if (!rateLimiter.tryAcquire(rateLimitKey)) {
            auditLogService.recordWithJsonResponse(AuditActionType.CONTRACT_PARSE, actor, null,
                    "(rate limited before file was read)", null, false, "Too many contract uploads");
            throw new TooManyContractUploadsException();
        }
        String requestDetail = file.getOriginalFilename() + " (" + file.getSize() + " bytes)";
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Uploaded file is empty");
            }
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                throw new ContractPdfUnreadableException(e);
            }
            String text = textExtractor.extractText(bytes);
            ExtractedContractFields regexFields = fieldExtractor.extract(text);
            AiExtractedPricingFields pricingFields = pricingAiExtractor.extract(text).orElse(null);
            ExtractedContractFields result = new ExtractedContractFields(
                    regexFields.eicCodes(), regexFields.registryCodeCandidates(), pricingFields);
            auditLogService.recordWithJsonResponse(AuditActionType.CONTRACT_PARSE, actor, null,
                    requestDetail, result, true, null);
            return result;
        } catch (RuntimeException e) {
            auditLogService.recordWithJsonResponse(AuditActionType.CONTRACT_PARSE, actor, null,
                    requestDetail, null, false, e.getMessage());
            throw e;
        }
    }
}
