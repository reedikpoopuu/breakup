package com.example.demo.contract;

import java.math.BigDecimal;

/**
 * Free-form pricing/contract-term fields pulled from contract text by an LLM (see
 * {@link ContractPricingAiExtractor}), as opposed to {@link ContractFieldExtractor}'s
 * fixed-format regex fields. Every field is nullable by design - the model is
 * instructed to return null rather than guess when something isn't clearly stated, so a
 * null here means "not found," not "extraction failed." Like everything else parsed in
 * this app, these are for the customer/admin to confirm, never auto-persisted.
 * <p>
 * {@code pricePerKwh} means different things depending on {@code contractType}: for
 * {@code FIXED} it is the flat price; for {@code SPOT} it is the margin added on top of
 * the live spot price, not a standalone price (see {@code ContractPricingAiExtractor}'s
 * system prompt). {@code monthlyFeeEur} is a flat monthly fee independent of
 * {@code contractType} - crossed with FIXED/SPOT this gives the four pricing structures
 * v1 supports (spot+margin with/without a monthly fee, fixed with/without one).
 * {@code termless} is only ever trusted when {@code contractType} is {@code SPOT} -
 * {@link ContractPricingAiExtractor#sanitize} discards it otherwise, and forces {@code
 * expiryDate} to null whenever it's true, since v1 only supports "no end date" for spot
 * contracts. A plan marketed as "fixed" whose price is actually a margin over a
 * periodically-republished reference rate (verified real example: Alexela's
 * "Pingevaba" - monthly-average spot base + margin + fee, contractually {@code
 * tähtajatu}) is expected to already be classified {@code SPOT} by the system prompt,
 * not carved out as a FIXED exception here. {@code earlyTerminationPenaltyEur} is the
 * mirror image: only ever trusted when {@code contractType} is {@code FIXED} and
 * forced null for {@code SPOT} (or unknown) - the penalty compensates the supplier for
 * breaking a hedge bought to guarantee the fixed price, which a spot contract has no
 * equivalent of. All monetary fields ({@code pricePerKwh}, {@code monthlyFeeEur},
 * {@code earlyTerminationPenaltyEur}) are excluding VAT, per the system prompt.
 */
public record AiExtractedPricingFields(
        String supplierName,
        String planName,
        BigDecimal pricePerKwh,
        BigDecimal monthlyFeeEur,
        String contractType,
        Boolean termless,
        String expiryDate,
        BigDecimal earlyTerminationPenaltyEur,
        String extractionNotes) {
}
