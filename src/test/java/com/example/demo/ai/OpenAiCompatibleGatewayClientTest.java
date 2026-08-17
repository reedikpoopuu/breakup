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

class OpenAiCompatibleGatewayClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiGatewayProperties configuredProperties(String baseUrl) {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setBaseUrl(baseUrl);
        properties.setApiKey("test-api-key");
        properties.setModel("test-model");
        return properties;
    }

    @Test
    void sendsTheOpenAiCompatibleRequestShapeAndParsesTheResponse() throws IOException {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            AtomicReference<JsonNode> seenBody = new AtomicReference<>();
            AtomicReference<String> seenAuthHeader = new AtomicReference<>();
            endpoint.handle("/chat/completions", exchange -> {
                seenAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
                try (InputStream is = exchange.getRequestBody()) {
                    seenBody.set(objectMapper.readTree(is));
                }
                byte[] responseBody = """
                        {"choices":[{"message":{"role":"assistant","content":"hello back"},"finish_reason":"stop"}],
                         "usage":{"prompt_tokens":12,"completion_tokens":3}}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBody.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(responseBody);
                }
            });

            AiGatewayProperties properties = configuredProperties(endpoint.baseUrl());
            OpenAiCompatibleGatewayClient client = new OpenAiCompatibleGatewayClient(properties, RestClient.builder());

            AiCompletionRequest request = new AiCompletionRequest(
                    List.of(AiMessage.system("be terse"), AiMessage.user("hello")), 256, 0.2);
            AiCompletionResponse response = client.complete(request);

            assertThat(seenAuthHeader.get()).isEqualTo("Bearer test-api-key");
            assertThat(seenBody.get().get("model").asText()).isEqualTo("test-model");
            assertThat(seenBody.get().get("max_tokens").asInt()).isEqualTo(256);
            assertThat(seenBody.get().get("messages").get(0).get("role").asText()).isEqualTo("system");
            assertThat(seenBody.get().get("messages").get(1).get("content").asText()).isEqualTo("hello");

            assertThat(response.content()).isEqualTo("hello back");
            assertThat(response.finishReason()).isEqualTo("stop");
            assertThat(response.promptTokens()).isEqualTo(12);
            assertThat(response.completionTokens()).isEqualTo(3);
        }
    }

    @Test
    void throwsANotConfiguredExceptionRatherThanCallingOutWhenUnset() {
        AiGatewayProperties properties = new AiGatewayProperties();
        OpenAiCompatibleGatewayClient client = new OpenAiCompatibleGatewayClient(properties, RestClient.builder());

        assertThatThrownBy(() -> client.complete(AiCompletionRequest.of(List.of(AiMessage.user("hi")))))
                .isInstanceOf(AiGatewayNotConfiguredException.class);
    }
}
