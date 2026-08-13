package com.example.demo.datahub;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Nord Pool day-ahead price API - public, needs no per-country credential. */
@ConfigurationProperties(prefix = "app.datahub.nordpool")
public class NordPoolProperties {

    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
