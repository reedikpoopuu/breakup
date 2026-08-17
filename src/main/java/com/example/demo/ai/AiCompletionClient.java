package com.example.demo.ai;

/**
 * Provider-agnostic chat-completion abstraction, with two implementations covering the
 * two wire formats in practical use: {@link OpenAiCompatibleGatewayClient} talks to any
 * gateway or router that speaks the OpenAI-compatible {@code /chat/completions} format -
 * a self-hosted LiteLLM proxy, OpenRouter, Azure OpenAI, a Bedrock/Vertex gateway, and
 * others all do, so swapping among those is a config change via {@link
 * AiGatewayProperties}, never a code change - while {@link AnthropicMessagesClient}
 * talks to Anthropic's own Messages API directly, for calling Claude without depending
 * on a compatibility-shim proxy in front of it.
 * <p>
 * This is scaffolding: nothing in this codebase calls it yet. It exists so the
 * free-form contract pricing-extraction step (the part {@link
 * com.example.demo.contract.ContractFieldExtractor}'s deterministic regexes
 * deliberately don't attempt) has a seam to depend on when it's built. Because two
 * beans implement this interface, that future caller needs to pick one explicitly
 * (inject the concrete class, or add a qualifier/property-driven selector at that
 * point) rather than autowiring {@code AiCompletionClient} unqualified.
 */
public interface AiCompletionClient {

    /** @throws AiGatewayNotConfiguredException if the gateway isn't configured yet */
    AiCompletionResponse complete(AiCompletionRequest request);
}
