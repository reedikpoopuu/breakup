package com.example.demo.ai;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Resolves which {@link AiCompletionClient} to actually use, since two beans implement
 * the interface (see its javadoc) and Spring can't pick between them on its own.
 * Depends on both concrete classes by name - rather than {@code List<AiCompletionClient>},
 * whose injection order isn't guaranteed - so preference order is explicit and
 * deterministic: {@link AnthropicMessagesClient} first (no compatibility-shim proxy in
 * the loop), falling back to {@link OpenAiCompatibleGatewayClient}. In practice only one
 * is expected to be configured at a time.
 */
@Component
public class AiCompletionClientRegistry {

    private final List<AiCompletionClient> orderedClients;

    public AiCompletionClientRegistry(AnthropicMessagesClient anthropicClient, OpenAiCompatibleGatewayClient gatewayClient) {
        this.orderedClients = List.of(anthropicClient, gatewayClient);
    }

    /** Empty when no provider is configured yet - callers should degrade gracefully, not fail the request. */
    public Optional<AiCompletionClient> resolve() {
        return orderedClients.stream().filter(AiCompletionClient::isConfigured).findFirst();
    }
}
