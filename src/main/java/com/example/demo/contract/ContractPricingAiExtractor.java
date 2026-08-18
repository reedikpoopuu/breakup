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
              "pricePerKwh": number (EUR per kWh) or null,
              "contractType": "FIXED", "SPOT", or null,
              "expiryDate": string in ISO yyyy-MM-dd format, or null,
              "earlyTerminationPenaltyEur": number or null,
              "extractionNotes": a short string flagging anything unclear or ambiguous, or ""
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
                List.of(AiMessage.system(SYSTEM_PROMPT), AiMessage.user(truncated)), 500, 0.0);

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
        String expiryDate = raw.expiryDate() != null && ISO_DATE.matcher(raw.expiryDate()).matches()
                ? raw.expiryDate() : null;
        return new AiExtractedPricingFields(
                truncate(raw.supplierName(), MAX_NAME_LENGTH),
                truncate(raw.planName(), MAX_NAME_LENGTH),
                raw.pricePerKwh(),
                contractType,
                expiryDate,
                raw.earlyTerminationPenaltyEur(),
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
