package com.example.demo.config;

import ee.sk.smartid.SmartIdClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

/**
 * Wires the official SK ID Solutions Smart-ID Java client. Defaults (see
 * application.properties) point at SK's public demo relying-party environment - no
 * contract required, only usable with SK's documented demo test identities - and are
 * overridden via app.smartid.* once a real relying party is issued.
 */
@Configuration
public class SmartIdConfig {

    // Each session-status check is a single, non-blocking HTTP call driven by the
    // browser's own poll loop (SmartIdController/SmartIdAuthController), not SK's
    // long-poll semantics - so ask SK to hold the connection open only briefly and bound
    // the HTTP client's own timeouts comfortably above that, so a stalled SK endpoint
    // fails fast instead of hanging the servlet thread indefinitely.
    private static final long SESSION_STATUS_SOCKET_OPEN_SECONDS = 3;
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 10_000;

    @Bean
    public SmartIdClient smartIdClient(
            @Value("${app.smartid.base-url}") String hostUrl,
            @Value("${app.smartid.relying-party-uuid}") String relyingPartyUuid,
            @Value("${app.smartid.relying-party-name}") String relyingPartyName) {
        SmartIdClient client = new SmartIdClient();
        client.setHostUrl(hostUrl);
        client.setRelyingPartyUUID(relyingPartyUuid);
        client.setRelyingPartyName(relyingPartyName);
        // sid.demo.sk.ee serves a publicly-trusted TLS certificate (not one of the
        // bundled cert-chain-of-trust certs used to validate the signed auth response) -
        // the client requires trust to be configured explicitly, so trust the JVM's
        // standard CA store rather than pinning a specific certificate.
        client.setTrustSslContext(defaultTrustSslContext());
        client.setNetworkConnectionConfig(new ClientConfig()
                .property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT_MILLIS)
                .property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT_MILLIS));
        // Must be set on the client, not client.getSmartIdConnector() directly: every
        // createAuthentication()/createSignature() call rebuilds the connector's
        // long-poll hold time from this field via createSessionStatusPoller(), silently
        // resetting it to unset if only the connector's own setter was called earlier -
        // verified against sid.demo.sk.ee (the poll call hung until READ_TIMEOUT_MILLIS
        // instead of returning within SESSION_STATUS_SOCKET_OPEN_SECONDS).
        client.setSessionStatusResponseSocketOpenTime(
                TimeUnit.SECONDS, SESSION_STATUS_SOCKET_OPEN_SECONDS);
        return client;
    }

    private static SSLContext defaultTrustSslContext() {
        try {
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to initialize default TLS trust for the Smart-ID client", e);
        }
    }
}
