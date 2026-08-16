package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import com.example.demo.datahub.support.MockHttpEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link EstfeedClient} against the real spec's contract: Keycloak-shaped
 * token response (snake_case access_token), the marketParticipantContext headers, and
 * the v2 metering-data endpoint shape - see EstfeedClient's javadoc for the sources.
 */
class EstfeedClientMockEndpointTest {

    private static EstfeedProperties properties(String baseUrl) {
        EstfeedProperties properties = new EstfeedProperties();
        properties.setBaseUrl(baseUrl);
        properties.setAuthBaseUrl(baseUrl);
        properties.setAuthRealm("estfeed-public");
        properties.setClientId("client");
        properties.setClientSecret("secret");
        properties.setMarketParticipantEic("38ZEE-1000009--Z");
        properties.setMarketParticipantRole("OPEN_SUPPLIER");
        return properties;
    }

    @Test
    void fetchesAndMapsConsumptionThroughTheKeycloakTokenFlow() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.respondJson("/realms/estfeed-public/protocol/openid-connect/token", 200,
                    "{\"access_token\":\"tok-123\",\"token_type\":\"Bearer\",\"expires_in\":300}");

            endpoint.handle("/api/v2/metering-data/electricity", exchange -> {
                assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer tok-123");
                assertThat(exchange.getRequestHeaders().getFirst("x-market-participant-eic")).isEqualTo("38ZEE-1000009--Z");
                assertThat(exchange.getRequestHeaders().getFirst("x-market-participant-role")).isEqualTo("OPEN_SUPPLIER");
                assertThat(exchange.getRequestHeaders().getFirst("x-commodity-type")).isEqualTo("ELECTRICITY");
                assertThat(exchange.getRequestHeaders().getFirst("x-document-identification")).isNotBlank();
                assertThat(exchange.getRequestURI().getQuery()).contains("meteringPointEics=EE1234");

                byte[] body = """
                        {"data":[
                          {"meterEic":"EE1234","periods":[
                            {"time":"2026-01-01T00:00:00Z","quantity":3.125}
                          ]}
                        ]}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            EstfeedClient client = new EstfeedClient(properties(endpoint.baseUrl()), RestClient.builder());

            List<ConsumptionRecord> records = client.fetchConsumption("EE1234", LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 2));

            assertThat(records).hasSize(1);
            ConsumptionRecord record = records.get(0);
            assertThat(record.getKwh()).isEqualByComparingTo(new BigDecimal("3.125"));
            assertThat(record.getIntervalStart().toString()).isEqualTo("2026-01-01T00:00:00Z");
            assertThat(record.getIntervalEnd().toString()).isEqualTo("2026-01-01T00:15:00Z");
            assertThat(record.getGranularity()).isEqualTo(Granularity.QUARTER_HOURLY);
            assertThat(record.getSource()).isEqualTo(DataHubSource.ESTFEED);
            assertThat(client.getCountry()).isEqualTo(CountryCode.EE);
        }
    }

    @Test
    void failsClosedWhenNotFullyConfigured() {
        EstfeedProperties properties = new EstfeedProperties();
        properties.setBaseUrl("http://localhost:1");
        properties.setClientId("client");
        properties.setClientSecret("secret");
        // marketParticipantEic/-Role deliberately left unset

        EstfeedClient client = new EstfeedClient(properties, RestClient.builder());

        assertThatThrownBy(() -> client.fetchConsumption("EE1234", LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DataHubNotConfiguredException.class);
    }

    @Test
    void propagatesUpstreamServerErrorRatherThanSwallowingIt() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.respondJson("/realms/estfeed-public/protocol/openid-connect/token", 200,
                    "{\"access_token\":\"tok-123\"}");
            endpoint.respondJson("/api/v2/metering-data/electricity", 503, "{\"error\":\"estfeed unavailable\"}");

            EstfeedClient client = new EstfeedClient(properties(endpoint.baseUrl()), RestClient.builder());

            assertThatThrownBy(() -> client.fetchConsumption("EE1234", LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 2)))
                    .isInstanceOf(RestClientResponseException.class);
        }
    }
}
