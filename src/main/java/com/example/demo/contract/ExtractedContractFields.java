package com.example.demo.contract;

import java.util.List;

/**
 * Full contract-parse result - never persisted automatically. Meant to be shown back to
 * the admin/customer for confirmation before anything is saved, same as every other
 * scraped/parsed value in this app. {@code eicCodes}/{@code registryCodeCandidates} come
 * from {@link ContractFieldExtractor}'s deterministic regexes; {@code pricingFields} is
 * {@link ContractPricingAiExtractor}'s free-form AI extraction, null whenever no AI
 * provider is configured or extraction failed - never treat null there as "this
 * contract has no pricing terms," only as "not extracted."
 */
public record ExtractedContractFields(List<String> eicCodes, List<RegistryCodeCandidate> registryCodeCandidates,
                                       AiExtractedPricingFields pricingFields) {
}
