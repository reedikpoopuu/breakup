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
 */
@Component
public class ContractPricingAiExtractor {

    private static final Logger log = LoggerFactory.getLogger(ContractPricingAiExtractor.class);

    /** Bounds cost/latency and keeps well under any provider's context window - a contract's pricing terms don't need the whole document. */
    private static final int MAX_CONTRACT_TEXT_CHARS = 12_000;

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
            return Optional.of(parse(response.content()));
        } catch (Exception e) {
            log.warn("AI pricing extraction failed, falling back to regex-only fields", e);
            return Optional.empty();
        }
    }

    private AiExtractedPricingFields parse(String rawContent) throws JsonProcessingException {
        String json = stripMarkdownFence(rawContent.trim());
        return objectMapper.readValue(json, AiExtractedPricingFields.class);
    }

    private static String stripMarkdownFence(String content) {
        if (!content.startsWith("```")) {
            return content;
        }
        String withoutOpenFence = content.replaceFirst("^```[a-zA-Z]*\\r?\\n?", "");
        return withoutOpenFence.replaceFirst("```\\s*$", "").trim();
    }
}
