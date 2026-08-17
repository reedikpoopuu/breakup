package com.example.demo.datahub;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * ESO Datahub (LT) connection settings - "DH API documentation for independent
 * supplier" v1.0.28 (datahub.eso.lt). Auth is a single static, long-lived Bearer JWT
 * issued directly by ESO's DSO team by e-mail (no client_id/client_secret, no OAuth2
 * grant, no token endpoint at all) - {@code token} replaces the OAuth2
 * client-credentials fields this class held before the spec was read in full.
 */
@ConfigurationProperties(prefix = "app.datahub.eso")
public class EsoProperties {

    private String baseUrl;
    private String token;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(token);
    }
}
