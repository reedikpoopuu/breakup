package com.example.demo.ai;

/**
 * Provider-agnostic chat-completion abstraction. The one real implementation
 * ({@link OpenAiCompatibleGatewayClient}) talks to any gateway or router that speaks
 * the OpenAI-compatible {@code /chat/completions} wire format - a self-hosted LiteLLM
 * proxy, OpenRouter, Azure OpenAI, a Bedrock/Vertex gateway, and others all do - so
 * swapping model or provider is a config change (base URL + API key + model name) via
 * {@link AiGatewayProperties}, never a code change or a new client class.
 * <p>
 * This is scaffolding: nothing in this codebase calls it yet. It exists so the
 * free-form contract pricing-extraction step (the part {@link
 * com.example.demo.contract.ContractFieldExtractor}'s deterministic regexes
 * deliberately don't attempt) has a seam to depend on when it's built, without that
 * step needing to know or care which provider sits behind the configured gateway.
 */
public interface AiCompletionClient {

    /** @throws AiGatewayNotConfiguredException if the gateway isn't configured yet */
    AiCompletionResponse complete(AiCompletionRequest request);
}
