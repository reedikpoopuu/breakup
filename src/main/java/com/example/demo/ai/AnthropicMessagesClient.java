package com.example.demo.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Talks to Anthropic's own Messages API ({@code POST /v1/messages}) directly - a
 * different wire shape from the OpenAI-compatible one {@link OpenAiCompatibleGatewayClient}
 * targets: auth is an {@code x-api-key} header (not {@code Authorization: Bearer}) plus
 * a required {@code anthropic-version} header, {@code system} is a top-level request
 * field rather than a "system"-role message, {@code max_tokens} is REQUIRED by the API
 * (unlike OpenAI's, where it's optional) so this defaults it when the caller doesn't
 * supply one, and response text lives in a {@code content} array of typed blocks rather
 * than a {@code choices} array.
 * <p>
 * A second {@link AiCompletionClient} bean alongside {@link OpenAiCompatibleGatewayClient}
 * is intentional - each targets a genuinely different wire format, so "provider-agnostic"
 * here means the call site depends on the interface and either implementation can be
 * selected, not that one HTTP shape covers every provider. Nothing currently autowires
 * the bare interface (see {@link AiCompletionClient}'s javadoc), so the two beans
 * coexist without ambiguity today; once a real caller exists it will need to pick one
 * explicitly (e.g. by injecting this class directly, or a qualifier/property-driven
 * selector at that point) rather than autowiring {@code AiCompletionClient} unqualified.
 */
@Component
public class AnthropicMessagesClient implements AiCompletionClient {

    private static final int DEFAULT_MAX_TOKENS = 1024;

    private record ContentBlock(String type, String text) {
    }

    private record AnthropicMessage(String role, String content) {
    }

    private record ThinkingConfig(String type) {
        static final ThinkingConfig DISABLED = new ThinkingConfig("disabled");
    }

    /**
     * {@code temperature} is {@code @JsonInclude(NON_NULL)}, not merely nullable: some
     * models (verified against {@code claude-sonnet-5}) reject the request outright if
     * the field is present at all, even as an explicit JSON {@code null} - "temperature
     * is deprecated for this model" / "temperature: Input should be a valid number"
     * respectively. It must be entirely absent from the wire body, so {@link #complete}
     * never forwards the caller's requested temperature here (see its comment).
     * <p>
     * {@code thinking} is always {@link ThinkingConfig#DISABLED} for the same
     * model-specific reason {@link #complete} drops temperature: verified against
     * {@code claude-sonnet-5}, which engages extended thinking by default on anything
     * beyond a trivial prompt, and that thinking shares the same {@code max_tokens}
     * budget as the actual answer. On a long enough prompt with a modest budget (the
     * contract-extraction caller uses 500), thinking can consume the entire budget and
     * leave no room for a {@code text} content block at all - confirmed directly against
     * the real API, where the same request produced a text block on some calls and only
     * a thinking block (no text, {@code stop_reason=max_tokens}) on others. Explicitly
     * disabling thinking (rather than just raising the token budget) made the same
     * request return a text block reliably across repeated calls - this app has no use
     * for exposed reasoning traces anyway, only the final structured answer.
     */
    private record MessagesRequestBody(String model, List<AnthropicMessage> messages,
                                        @JsonProperty("max_tokens") int maxTokens,
                                        String system,
                                        @JsonInclude(JsonInclude.Include.NON_NULL) Double temperature,
                                        ThinkingConfig thinking) {
    }

    private record Usage(@JsonProperty("input_tokens") Integer inputTokens,
                          @JsonProperty("output_tokens") Integer outputTokens) {
    }

    private record MessagesResponseBody(List<ContentBlock> content,
                                         @JsonProperty("stop_reason") String stopReason,
                                         Usage usage) {
    }

    private final AnthropicProperties properties;
    private final RestClient.Builder restClientBuilder;

    public AnthropicMessagesClient(AnthropicProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public AiCompletionResponse complete(AiCompletionRequest request) {
        if (!properties.isConfigured()) {
            throw new AiGatewayNotConfiguredException("set app.ai.anthropic.api-key / model");
        }

        String system = request.messages().stream()
                .filter(m -> "system".equals(m.role()))
                .map(AiMessage::content)
                .findFirst()
                .orElse(null);
        List<AnthropicMessage> messages = request.messages().stream()
                .filter(m -> !"system".equals(m.role()))
                .map(m -> new AnthropicMessage(m.role(), m.content()))
                .toList();
        int maxTokens = request.maxTokens() != null ? request.maxTokens() : DEFAULT_MAX_TOKENS;

        // request.temperature() is deliberately dropped rather than forwarded - see
        // MessagesRequestBody's javadoc: at least one real model rejects the field
        // outright regardless of value, so this client can't honor a caller's requested
        // temperature at all today, only omit it.
        MessagesRequestBody body = new MessagesRequestBody(
                properties.getModel(), messages, maxTokens, system, null, ThinkingConfig.DISABLED);

        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        MessagesResponseBody response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", properties.getApiKey())
                .header("anthropic-version", properties.getApiVersion())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(MessagesResponseBody.class);

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new IllegalStateException("Anthropic API returned no content");
        }
        String text = response.content().stream()
                .filter(block -> "text".equals(block.type()))
                .map(ContentBlock::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Anthropic API returned no text content block"));

        Usage usage = response.usage();
        return new AiCompletionResponse(
                text,
                response.stopReason(),
                usage != null ? usage.inputTokens() : null,
                usage != null ? usage.outputTokens() : null);
    }
}
