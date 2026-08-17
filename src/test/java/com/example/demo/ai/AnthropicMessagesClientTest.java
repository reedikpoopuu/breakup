package com.example.demo.ai;

import com.example.demo.datahub.support.MockHttpEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicMessagesClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnthropicProperties configuredProperties(String baseUrl) {
        AnthropicProperties properties = new AnthropicProperties();
        properties.setBaseUrl(baseUrl);
        properties.setApiKey("test-api-key");
        properties.setModel("claude-test-model");
        return properties;
    }

    @Test
    void sendsTheAnthropicMessagesRequestShapeAndParsesTheResponse() throws IOException {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            AtomicReference<JsonNode> seenBody = new AtomicReference<>();
            AtomicReference<String> seenApiKeyHeader = new AtomicReference<>();
            AtomicReference<String> seenVersionHeader = new AtomicReference<>();
            endpoint.handle("/v1/messages", exchange -> {
                seenApiKeyHeader.set(exchange.getRequestHeaders().getFirst("x-api-key"));
                seenVersionHeader.set(exchange.getRequestHeaders().getFirst("anthropic-version"));
                try (InputStream is = exchange.getRequestBody()) {
                    seenBody.set(objectMapper.readTree(is));
                }
                byte[] responseBody = """
                        {"content":[{"type":"text","text":"hello back"}],
                         "stop_reason":"end_turn",
                         "usage":{"input_tokens":12,"output_tokens":3}}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBody.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(responseBody);
                }
            });

            AnthropicProperties properties = configuredProperties(endpoint.baseUrl());
            AnthropicMessagesClient client = new AnthropicMessagesClient(properties, RestClient.builder());

            AiCompletionRequest request = new AiCompletionRequest(
                    List.of(AiMessage.system("be terse"), AiMessage.user("hello")), 256, 0.2);
            AiCompletionResponse response = client.complete(request);

            assertThat(seenApiKeyHeader.get()).isEqualTo("test-api-key");
            assertThat(seenVersionHeader.get()).isEqualTo("2023-06-01");
            assertThat(seenBody.get().get("model").asText()).isEqualTo("claude-test-model");
            assertThat(seenBody.get().get("max_tokens").asInt()).isEqualTo(256);
            assertThat(seenBody.get().get("system").asText()).isEqualTo("be terse");
            assertThat(seenBody.get().get("messages")).hasSize(1);
            assertThat(seenBody.get().get("messages").get(0).get("role").asText()).isEqualTo("user");
            assertThat(seenBody.get().get("messages").get(0).get("content").asText()).isEqualTo("hello");

            assertThat(response.content()).isEqualTo("hello back");
            assertThat(response.finishReason()).isEqualTo("end_turn");
            assertThat(response.promptTokens()).isEqualTo(12);
            assertThat(response.completionTokens()).isEqualTo(3);
        }
    }

    @Test
    void defaultsMaxTokensWhenTheCallerDoesNotSupplyOne() throws IOException {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            AtomicReference<JsonNode> seenBody = new AtomicReference<>();
            endpoint.handle("/v1/messages", exchange -> {
                try (InputStream is = exchange.getRequestBody()) {
                    seenBody.set(objectMapper.readTree(is));
                }
                byte[] responseBody = """
                        {"content":[{"type":"text","text":"ok"}],"stop_reason":"end_turn","usage":{"input_tokens":1,"output_tokens":1}}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBody.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(responseBody);
                }
            });

            AnthropicProperties properties = configuredProperties(endpoint.baseUrl());
            AnthropicMessagesClient client = new AnthropicMessagesClient(properties, RestClient.builder());

            client.complete(AiCompletionRequest.of(List.of(AiMessage.user("hi"))));

            assertThat(seenBody.get().get("max_tokens").asInt()).isEqualTo(1024);
        }
    }

    @Test
    void throwsANotConfiguredExceptionRatherThanCallingOutWhenUnset() {
        AnthropicProperties properties = new AnthropicProperties();
        AnthropicMessagesClient client = new AnthropicMessagesClient(properties, RestClient.builder());

        assertThatThrownBy(() -> client.complete(AiCompletionRequest.of(List.of(AiMessage.user("hi")))))
                .isInstanceOf(AiGatewayNotConfiguredException.class)
                .hasMessageContaining("app.ai.anthropic");
    }
}
