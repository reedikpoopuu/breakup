package com.example.demo.datahub;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * STEP / Sadales tīkls (LV) connection settings - "Single Data Exchange Standard"
 * v1.6 (sadalestikls.lv/storage/app/media/platforma/Single_Data_Exchange_Standard_v1.6_ENG.pdf).
 * Real auth is username/password against a SOAP "Auth service" yielding a JWT
 * ({@code Authorization: Bearer <JWT>}) - not the static API key this class held
 * before the spec was read in full. {@code username}/{@code password} are the
 * "system:system user" credentials, activated once via the spec's {@code
 * ChangeCredentials} flow (out of band - see StepClient) before they work here.
 */
@ConfigurationProperties(prefix = "app.datahub.step")
public class StepProperties {

    private String baseUrl;
    private String authBaseUrl;
    private String username;
    private String password;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthBaseUrl() {
        return authBaseUrl;
    }

    public void setAuthBaseUrl(String authBaseUrl) {
        this.authBaseUrl = authBaseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(username) && StringUtils.hasText(password) && StringUtils.hasText(authBaseUrl);
    }
}
