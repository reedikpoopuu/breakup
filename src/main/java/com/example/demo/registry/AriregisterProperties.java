package com.example.demo.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Estonian e-Business Register (Ariregister) open-data connection settings for the
 * {@code arireg.esindus_v1} rights-of-representation query - see
 * {@link AriregisterEsindusClient} for the query itself and what's confirmed vs. not.
 * Unlike the DataHub adapters, {@code baseUrl} has no confirmed real default: RIK's
 * public documentation page for this query (avaandmed.ariregister.rik.ee) states the
 * request/response shape but does not publish the SOAP endpoint hostname or a
 * self-service signup - per RIK's own abiinfo.rik.ee FAQ, API/XML service access
 * requires signing a contract (start at ariregister.rik.ee/eng/contract, or via
 * klienditugi@rik.ee / +372 680 3160). Set the three properties below explicitly via
 * env vars once that contract yields real credentials and an endpoint - see "Getting
 * real credentials" in {@code docs/api-specs/business-registry/ee.md}.
 */
@ConfigurationProperties(prefix = "app.registry.ariregister")
public class AriregisterProperties {

    private String baseUrl;
    private String username;
    private String password;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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
        return StringUtils.hasText(baseUrl) && StringUtils.hasText(username) && StringUtils.hasText(password);
    }
}
