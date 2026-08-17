package com.example.demo.ai;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class AiCompletionClientRegistryTest {

    private AnthropicMessagesClient unconfiguredAnthropicClient() {
        return new AnthropicMessagesClient(new AnthropicProperties(), RestClient.builder());
    }

    private OpenAiCompatibleGatewayClient unconfiguredGatewayClient() {
        return new OpenAiCompatibleGatewayClient(new AiGatewayProperties(), RestClient.builder());
    }

    private AnthropicMessagesClient configuredAnthropicClient() {
        AnthropicProperties properties = new AnthropicProperties();
        properties.setApiKey("key");
        properties.setModel("model");
        return new AnthropicMessagesClient(properties, RestClient.builder());
    }

    private OpenAiCompatibleGatewayClient configuredGatewayClient() {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setBaseUrl("http://localhost:1");
        properties.setApiKey("key");
        properties.setModel("model");
        return new OpenAiCompatibleGatewayClient(properties, RestClient.builder());
    }

    @Test
    void resolvesEmptyWhenNeitherProviderIsConfigured() {
        AiCompletionClientRegistry registry = new AiCompletionClientRegistry(
                unconfiguredAnthropicClient(), unconfiguredGatewayClient());

        assertThat(registry.resolve()).isEmpty();
    }

    @Test
    void prefersAnthropicWhenBothAreConfigured() {
        AnthropicMessagesClient anthropic = configuredAnthropicClient();
        OpenAiCompatibleGatewayClient gateway = configuredGatewayClient();
        AiCompletionClientRegistry registry = new AiCompletionClientRegistry(anthropic, gateway);

        assertThat(registry.resolve()).contains(anthropic);
    }

    @Test
    void fallsBackToTheGatewayWhenOnlyItIsConfigured() {
        OpenAiCompatibleGatewayClient gateway = configuredGatewayClient();
        AiCompletionClientRegistry registry = new AiCompletionClientRegistry(unconfiguredAnthropicClient(), gateway);

        assertThat(registry.resolve()).contains(gateway);
    }
}
