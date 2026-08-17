package com.example.demo.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Connection settings for {@link AnthropicMessagesClient} - Anthropic's own Messages API
 * (api.anthropic.com), a different wire shape from the OpenAI-compatible one {@link
 * AiGatewayProperties} configures. {@code baseUrl} defaults to the real, fixed API host
 * since (unlike a generic gateway) there's only one; {@code apiKey}/{@code model} still
 * need setting explicitly.
 */
@ConfigurationProperties(prefix = "app.ai.anthropic")
public class AnthropicProperties {

    private String baseUrl = "https://api.anthropic.com";
    private String apiKey;
    private String model;
    private String apiVersion = "2023-06-01";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(baseUrl) && StringUtils.hasText(apiKey) && StringUtils.hasText(model);
    }
}
