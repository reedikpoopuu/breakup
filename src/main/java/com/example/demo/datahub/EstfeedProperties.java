package com.example.demo.datahub;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Elering Estfeed (EE) connection settings - github.com/Elering/estfeed-datahub-docs.
 * {@code marketParticipantEic}/{@code marketParticipantRole} are this application's own
 * market-participant identity (who Elering thinks is calling), not per-request data -
 * see {@link EstfeedClient} for how they're used.
 */
@ConfigurationProperties(prefix = "app.datahub.estfeed")
public class EstfeedProperties {

    private String baseUrl;
    private String authBaseUrl;
    private String authRealm;
    private String clientId;
    private String clientSecret;
    private String marketParticipantEic;
    private String marketParticipantRole;

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

    public String getAuthRealm() {
        return authRealm;
    }

    public void setAuthRealm(String authRealm) {
        this.authRealm = authRealm;
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

    public String getMarketParticipantEic() {
        return marketParticipantEic;
    }

    public void setMarketParticipantEic(String marketParticipantEic) {
        this.marketParticipantEic = marketParticipantEic;
    }

    public String getMarketParticipantRole() {
        return marketParticipantRole;
    }

    public void setMarketParticipantRole(String marketParticipantRole) {
        this.marketParticipantRole = marketParticipantRole;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret)
                && StringUtils.hasText(marketParticipantEic) && StringUtils.hasText(marketParticipantRole);
    }
}
