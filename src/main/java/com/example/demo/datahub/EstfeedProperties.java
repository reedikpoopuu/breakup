package com.example.demo.datahub;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Elering Estfeed (EE) connection settings - github.com/Elering/estfeed-datahub-docs. */
@ConfigurationProperties(prefix = "app.datahub.estfeed")
public class EstfeedProperties {

    private String baseUrl;
    private String clientId;
    private String clientSecret;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
