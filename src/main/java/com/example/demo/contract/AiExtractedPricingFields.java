package com.example.demo.contract;

import java.math.BigDecimal;

/**
 * Free-form pricing/contract-term fields pulled from contract text by an LLM (see
 * {@link ContractPricingAiExtractor}), as opposed to {@link ContractFieldExtractor}'s
 * fixed-format regex fields. Every field is nullable by design - the model is
 * instructed to return null rather than guess when something isn't clearly stated, so a
 * null here means "not found," not "extraction failed." Like everything else parsed in
 * this app, these are for the customer/admin to confirm, never auto-persisted.
 */
public record AiExtractedPricingFields(
        String supplierName,
        String planName,
        BigDecimal pricePerKwh,
        String contractType,
        String expiryDate,
        BigDecimal earlyTerminationPenaltyEur,
        String extractionNotes) {
}
