package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import com.example.demo.datahub.support.MockHttpEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstfeedClientMockEndpointTest {

    @Test
    void fetchesAndMapsConsumptionThroughOAuthTokenFlow() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.respondJson("/oauth/token", 200, "{\"accessToken\":\"tok-123\"}");
            endpoint.respondJson("/api/v1/consumption", 200, """
                    {"intervals":[
                      {"start":"2026-01-01T00:00:00Z","end":"2026-01-01T01:00:00Z","kwh":12.5}
                    ]}
                    """);

            EstfeedProperties properties = new EstfeedProperties();
            properties.setBaseUrl(endpoint.baseUrl());
            properties.setClientId("client");
            properties.setClientSecret("secret");

            EstfeedClient client = new EstfeedClient(properties, RestClient.builder());

            List<ConsumptionRecord> records = client.fetchConsumption("EE1234", LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 2));

            assertThat(records).hasSize(1);
            ConsumptionRecord record = records.get(0);
            assertThat(record.getKwh()).isEqualByComparingTo(new BigDecimal("12.5"));
            assertThat(record.getGranularity()).isEqualTo(Granularity.HOURLY);
            assertThat(record.getSource()).isEqualTo(DataHubSource.ESTFEED);
            assertThat(client.getCountry()).isEqualTo(CountryCode.EE);
        }
    }

    @Test
    void failsClosedWhenCredentialsAreNotConfigured() {
        EstfeedProperties properties = new EstfeedProperties();
        properties.setBaseUrl("http://localhost:1");

        EstfeedClient client = new EstfeedClient(properties, RestClient.builder());

        assertThatThrownBy(() -> client.fetchConsumption("EE1234", LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DataHubNotConfiguredException.class);
    }

    @Test
    void propagatesUpstreamServerErrorRatherThanSwallowingIt() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.respondJson("/oauth/token", 200, "{\"accessToken\":\"tok-123\"}");
            endpoint.respondJson("/api/v1/consumption", 503, "{\"error\":\"estfeed unavailable\"}");

            EstfeedProperties properties = new EstfeedProperties();
            properties.setBaseUrl(endpoint.baseUrl());
            properties.setClientId("client");
            properties.setClientSecret("secret");

            EstfeedClient client = new EstfeedClient(properties, RestClient.builder());

            assertThatThrownBy(() -> client.fetchConsumption("EE1234", LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 2)))
                    .isInstanceOf(RestClientResponseException.class);
        }
    }
}
