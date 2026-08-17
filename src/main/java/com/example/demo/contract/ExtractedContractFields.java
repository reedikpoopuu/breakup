package com.example.demo.contract;

import java.util.List;

/**
 * Deterministic, regex-only extraction result - never persisted automatically. Meant
 * to be shown back to the admin/customer for confirmation before anything is saved,
 * same as every other scraped/parsed value in this app.
 */
public record ExtractedContractFields(List<String> eicCodes, List<RegistryCodeCandidate> registryCodeCandidates) {
}
