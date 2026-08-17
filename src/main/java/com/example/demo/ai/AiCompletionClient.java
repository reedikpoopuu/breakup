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
 * Because two beans implement this interface, nothing autowires the bare interface
 * directly - {@link AiCompletionClientRegistry} depends on both concrete classes by
 * name and resolves whichever one is actually configured, in a fixed preference order.
 * {@link com.example.demo.contract.ContractPricingAiExtractor} is the one real caller,
 * going through that registry rather than either client directly.
 */
public interface AiCompletionClient {

    /** True once the client has everything it needs (base URL/API key/model) to be called. */
    boolean isConfigured();

    /** @throws AiGatewayNotConfiguredException if the gateway isn't configured yet */
    AiCompletionResponse complete(AiCompletionRequest request);
}
