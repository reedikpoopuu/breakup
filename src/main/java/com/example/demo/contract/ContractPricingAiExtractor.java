package com.example.demo.contract;

import com.example.demo.ai.AiCompletionClient;
import com.example.demo.ai.AiCompletionClientRegistry;
import com.example.demo.ai.AiCompletionRequest;
import com.example.demo.ai.AiCompletionResponse;
import com.example.demo.ai.AiMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Free-form pricing/contract-term extraction via whichever {@link AiCompletionClient}
 * {@link AiCompletionClientRegistry} resolves - step 3 of contract field extraction,
 * covering what {@link ContractFieldExtractor}'s deterministic regexes deliberately
 * don't attempt (price, plan name, contract type, expiry, penalty: free-form text with
 * no fixed format across suppliers/languages).
 * <p>
 * Degrades gracefully rather than failing the upload: if no provider is configured, or
 * the call fails, or the model's response isn't parseable, this returns {@link
 * Optional#empty()} and logs a warning - {@link ContractUploadController} still returns
 * the regex fields either way. Nothing here is ever persisted automatically.
 * <p>
 * The prompt above asks the model to stick to a fixed shape, but nothing stops a
 * malicious contract's text from containing a prompt-injection payload ("ignore the
 * above instructions...") that steers the response - {@code pricePerKwh}/{@code
 * earlyTerminationPenaltyEur} are typed {@link java.math.BigDecimal}, so Jackson already
 * rejects non-numeric injection there for free, but the free-form string fields have no
 * such guarantee. {@link #sanitize} is the actual control: it doesn't trust the model's
 * adherence to the prompt for {@code contractType} (constrained to the literal enum
 * shape, anything else becomes null) or for string length (capped, since an unbounded
 * string here flows straight into the audit log's CSV export).
 */
@Component
public class ContractPricingAiExtractor {

    private static final Logger log = LoggerFactory.getLogger(ContractPricingAiExtractor.class);

    /** Bounds cost/latency and keeps well under any provider's context window - a contract's pricing terms don't need the whole document. */
    private static final int MAX_CONTRACT_TEXT_CHARS = 12_000;

    private static final Set<String> ALLOWED_CONTRACT_TYPES = Set.of("FIXED", "SPOT");
    private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_NOTES_LENGTH = 500;

    private static final String SYSTEM_PROMPT = """
            You extract structured pricing and contract-term data from an electricity supply \
            contract for a Baltic (Estonia/Latvia/Lithuania) energy-switching platform. Read the \
            contract text and reply with ONLY a single JSON object - no markdown fences, no \
            commentary before or after it - with exactly these fields:
            {
              "supplierName": string or null,
              "planName": string or null,
              "pricePerKwh": number or null - for a FIXED contract, the flat price per kWh; \
            for a SPOT contract, the margin added on top of the live spot price per kWh, NOT a \
            standalone price. ALWAYS the price excluding VAT/käibemaks/PVN/PVM, even if the \
            contract only shows the VAT-inclusive figure prominently - if both are shown, use \
            the excl.-VAT one,
              "monthlyFeeEur": number or null - a flat monthly fee charged in addition to the \
            per-kWh price/margin, independent of contract type; null if no such fee is stated. \
            Also excluding VAT, same rule as pricePerKwh,
              "contractType": "FIXED", "SPOT", or null. Note some plans marketed as a \
            "fixed price" are actually SPOT here: if the price is described as a periodically \
            published average/reference market rate plus a margin (e.g. re-set monthly based \
            on that period's average market price), classify as SPOT even if the marketing \
            copy uses the word "fixed" - what matters is whether the number is a standalone \
            flat rate or a margin over a market-derived reference,
              "termless": true only if the contract explicitly has no fixed end date and \
            continues until cancelled (e.g. Estonian "tähtajatu") AND contractType is "SPOT" - \
            otherwise false. Never true for a FIXED contract, even if it also reads as \
            indefinite - v1 does not support that combination,
              "expiryDate": string in ISO yyyy-MM-dd format, or null - omit/null whenever \
            termless is true,
              "earlyTerminationPenaltyEur": number or null - ONLY applicable to a FIXED \
            contract (the supplier has hedged the fixed price with underlying purchased assets, \
            which is what the penalty compensates for breaking). A SPOT contract has no such \
            hedge, so this must always be null for SPOT even if the text mentions some other \
            kind of exit fee - do not conflate the two. Also excluding VAT,
              "extractionNotes": a short string flagging anything unclear or ambiguous, or "" - \
            if the contract is SPOT and the text nonetheless states an early-termination \
            penalty (unusual), mention it here rather than silently dropping it, since \
            earlyTerminationPenaltyEur is forced null for SPOT regardless of what you put there
            }
            If a field is not clearly stated in the text, use null rather than guessing - never \
            fabricate a value.""";

    private final AiCompletionClientRegistry registry;
    private final ObjectMapper objectMapper;

    public ContractPricingAiExtractor(AiCompletionClientRegistry registry) {
        this.registry = registry;
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Optional<AiExtractedPricingFields> extract(String contractText) {
        Optional<AiCompletionClient> client = registry.resolve();
        if (client.isEmpty()) {
            return Optional.empty();
        }

        String truncated = contractText.length() > MAX_CONTRACT_TEXT_CHARS
                ? contractText.substring(0, MAX_CONTRACT_TEXT_CHARS)
                : contractText;
        AiCompletionRequest request = new AiCompletionRequest(
                List.of(AiMessage.system(SYSTEM_PROMPT), AiMessage.user(truncated)), 600, 0.0);

        try {
            AiCompletionResponse response = client.get().complete(request);
            return Optional.of(sanitize(parse(response.content())));
        } catch (Exception e) {
            log.warn("AI pricing extraction failed, falling back to regex-only fields", e);
            return Optional.empty();
        }
    }

    private AiExtractedPricingFields parse(String rawContent) throws JsonProcessingException {
        String json = stripMarkdownFence(rawContent.trim());
        return objectMapper.readValue(json, AiExtractedPricingFields.class);
    }

    private static AiExtractedPricingFields sanitize(AiExtractedPricingFields raw) {
        String contractType = raw.contractType() != null && ALLOWED_CONTRACT_TYPES.contains(raw.contractType())
                ? raw.contractType() : null;
        // termless is only ever trusted for SPOT - v1 has no "no end date" concept for FIXED,
        // whatever the model claims (see AiExtractedPricingFields' javadoc). A plan marketed as
        // "fixed" that's actually a margin over a periodically-republished reference price (e.g.
        // Alexela's "Pingevaba" - monthly-average spot base + margin + fee, contractually
        // tähtajatu) should already have been classified SPOT by the prompt above, precisely so
        // it lands here rather than needing a FIXED-can-be-termless carve-out. And whenever it IS
        // termless, expiryDate is forced null too: the two are mutually exclusive, and the model
        // isn't trusted to have enforced that itself.
        boolean termless = Boolean.TRUE.equals(raw.termless()) && "SPOT".equals(contractType);
        String expiryDate = termless ? null
                : raw.expiryDate() != null && ISO_DATE.matcher(raw.expiryDate()).matches() ? raw.expiryDate() : null;
        // Early-termination penalties compensate the supplier for breaking a hedge bought to
        // guarantee a fixed price - a SPOT contract has no such hedge, so it can never carry
        // one here regardless of what the model claims, symmetric to the termless/SPOT rule above.
        BigDecimal earlyTerminationPenaltyEur = "FIXED".equals(contractType) ? raw.earlyTerminationPenaltyEur() : null;
        return new AiExtractedPricingFields(
                truncate(raw.supplierName(), MAX_NAME_LENGTH),
                truncate(raw.planName(), MAX_NAME_LENGTH),
                raw.pricePerKwh(),
                raw.monthlyFeeEur(),
                contractType,
                termless,
                expiryDate,
                earlyTerminationPenaltyEur,
                truncate(raw.extractionNotes(), MAX_NOTES_LENGTH));
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String stripMarkdownFence(String content) {
        if (!content.startsWith("```")) {
            return content;
        }
        String withoutOpenFence = content.replaceFirst("^```[a-zA-Z]*\\r?\\n?", "");
        return withoutOpenFence.replaceFirst("```\\s*$", "").trim();
    }
}
