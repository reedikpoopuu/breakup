package com.example.demo.datahub;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * STEP / Sadales tikls (LV) connection settings - "Vienotais datu apmainas standarts"
 * v1.5. Exact auth model (API key vs. mTLS) to be confirmed once the standard PDF is
 * read in full; API key is the placeholder per ARCH_SPEC.md section 3.2.
 */
@ConfigurationProperties(prefix = "app.datahub.step")
public class StepProperties {

    private String baseUrl;
    private String apiKey;

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

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
