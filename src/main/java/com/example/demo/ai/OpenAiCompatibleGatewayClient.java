package com.example.demo.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Talks to any gateway/router that speaks the OpenAI-compatible {@code
 * /chat/completions} format - see {@link AiCompletionClient}'s javadoc for why that
 * makes this provider-agnostic in practice (OpenRouter, a LiteLLM proxy, Azure OpenAI,
 * a Bedrock/Vertex gateway, and plenty of others all speak it). No provider-specific
 * code lives here - only this one wire shape.
 */
@Component
public class OpenAiCompatibleGatewayClient implements AiCompletionClient {

    private record ChatMessage(String role, String content) {
    }

    private record ChatRequestBody(String model, List<ChatMessage> messages,
                                    @JsonProperty("max_tokens") Integer maxTokens,
                                    Double temperature) {
    }

    private record ChatChoice(ChatMessage message, @JsonProperty("finish_reason") String finishReason) {
    }

    private record ChatUsage(@JsonProperty("prompt_tokens") Integer promptTokens,
                              @JsonProperty("completion_tokens") Integer completionTokens) {
    }

    private record ChatResponseBody(List<ChatChoice> choices, ChatUsage usage) {
    }

    private final AiGatewayProperties properties;
    private final RestClient.Builder restClientBuilder;

    public OpenAiCompatibleGatewayClient(AiGatewayProperties properties, RestClient.Builder restClientBuilder) {
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
            throw new AiGatewayNotConfiguredException();
        }
        List<ChatMessage> messages = request.messages().stream()
                .map(m -> new ChatMessage(m.role(), m.content()))
                .toList();
        ChatRequestBody body = new ChatRequestBody(properties.getModel(), messages,
                request.maxTokens(), request.temperature());

        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        ChatResponseBody response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatResponseBody.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("AI gateway returned no choices");
        }
        ChatChoice first = response.choices().get(0);
        ChatUsage usage = response.usage();
        return new AiCompletionResponse(
                first.message().content(),
                first.finishReason(),
                usage != null ? usage.promptTokens() : null,
                usage != null ? usage.completionTokens() : null);
    }
}
