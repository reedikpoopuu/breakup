package com.example.demo.contract;

import com.example.demo.common.CountryCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, regex-only extraction of EIC codes and company registry codes from
 * contract text - step 2 of contract field extraction (step 1 is
 * {@link ContractPdfTextExtractor}, step 3 is {@link ContractPricingAiExtractor}). No AI
 * call involved here; {@code pricingFields} on the returned {@link
 * ExtractedContractFields} is always null from this class - the free-form
 * pricing/contract-terms fields don't have a fixed format the way these two do, so
 * {@link ContractUploadController} fills that field in separately via step 3.
 * <p>
 * <b>EIC codes (confirmed):</b> {@code \d{2}[XYZWTVA][A-Z0-9-]{12}[A-Z0-9]}, a real
 * 16-character pattern transcribed from Elering's live Estfeed OpenAPI document
 * (datahub.elering.ee/v3/api-docs, the {@code meteringPointEics} parameter - see
 * {@code EstfeedClient}'s javadoc for how that was verified). EIC is an EU-wide
 * ENTSO-E standard, not country-specific, so one pattern covers EE/LV/LT contracts
 * alike.
 * <p>
 * <b>Registry codes (best-effort, NOT verified against a real contract):</b> no sample
 * contract PDF was available to check these against, unlike the EIC pattern above.
 * Matching is anchored to each country's own official term for a company registry
 * number (Estonian "Registrikood", Latvian "Reģistrācijas numurs", Lithuanian "Įmonės
 * kodas") rather than a bare digit-count, since an unanchored "8 digits somewhere in
 * the PDF" would also match phone numbers, dates, and invoice numbers. Digit counts
 * (EE 8, LV 11, LT 9) come from this app's own seeded demo data
 * ({@code index.html}'s company templates), which is at least self-consistent, but
 * still a candidate for human confirmation before use, not a settled result.
 */
@Component
public class ContractFieldExtractor {

    private static final Pattern EIC = Pattern.compile("\\b\\d{2}[XYZWTVA][A-Z0-9-]{12}[A-Z0-9]\\b");

    private record RegistryPattern(CountryCode country, Pattern pattern) {
    }

    // (?iu), not just (?i): Java's CASE_INSENSITIVE alone only folds ASCII, so it would
    // never match the accented "Į" in "Įmonės kodas" against the lowercase "į" below -
    // UNICODE_CASE is required for that fold to cover Baltic diacritics.
    private static final List<RegistryPattern> REGISTRY_PATTERNS = List.of(
            new RegistryPattern(CountryCode.EE, Pattern.compile("(?iu)registrikood\\D{0,10}(\\d{8})")),
            new RegistryPattern(CountryCode.LV, Pattern.compile("(?iu)re[gģ]istr[āa]cijas\\s*numurs\\D{0,10}(\\d{11})")),
            new RegistryPattern(CountryCode.LT, Pattern.compile("(?iu)[įi]mon[ėe]s\\s*kodas\\D{0,10}(\\d{9})"))
    );

    public ExtractedContractFields extract(String text) {
        List<String> eicCodes = new ArrayList<>();
        Matcher eicMatcher = EIC.matcher(text);
        while (eicMatcher.find()) {
            String code = eicMatcher.group();
            if (!eicCodes.contains(code)) {
                eicCodes.add(code);
            }
        }

        List<RegistryCodeCandidate> registryCandidates = new ArrayList<>();
        for (RegistryPattern registryPattern : REGISTRY_PATTERNS) {
            Matcher matcher = registryPattern.pattern().matcher(text);
            while (matcher.find()) {
                RegistryCodeCandidate candidate = new RegistryCodeCandidate(registryPattern.country(), matcher.group(1));
                if (!registryCandidates.contains(candidate)) {
                    registryCandidates.add(candidate);
                }
            }
        }

        return new ExtractedContractFields(eicCodes, registryCandidates, null);
    }
}
