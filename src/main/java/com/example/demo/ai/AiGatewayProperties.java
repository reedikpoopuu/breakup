package com.example.demo.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Connection settings for {@link OpenAiCompatibleGatewayClient}. {@code baseUrl} points
 * at whichever OpenAI-compatible gateway/router is in front of the actual model
 * provider(s) - deliberately provider-agnostic, see {@link AiCompletionClient}.
 */
@ConfigurationProperties(prefix = "app.ai.gateway")
public class AiGatewayProperties {

    private String baseUrl;
    private String apiKey;
    private String model;

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

    public boolean isConfigured() {
        return StringUtils.hasText(baseUrl) && StringUtils.hasText(apiKey) && StringUtils.hasText(model);
    }
}
