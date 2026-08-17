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

class EsoClientMockEndpointTest {

    @Test
    void fetchesAndMapsConsumptionThroughOAuthTokenFlow() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.respondJson("/oauth/token", 200, "{\"accessToken\":\"tok-456\"}");
            endpoint.respondJson("/api/v1/consumption", 200, """
                    {"intervals":[
                      {"start":"2026-01-01T00:00:00Z","end":"2026-02-01T00:00:00Z","kwh":610.0}
                    ]}
                    """);

            EsoProperties properties = new EsoProperties();
            properties.setBaseUrl(endpoint.baseUrl());
            properties.setClientId("client");
            properties.setClientSecret("secret");

            EsoClient client = new EsoClient(properties, RestClient.builder());

            List<ConsumptionRecord> records = client.fetchConsumption("CUSTOMER1", "LT1234", true,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

            assertThat(records).hasSize(1);
            assertThat(records.get(0).getKwh()).isEqualByComparingTo(new BigDecimal("610.0"));
            assertThat(records.get(0).getSource()).isEqualTo(DataHubSource.ESO);
            assertThat(client.getCountry()).isEqualTo(CountryCode.LT);
        }
    }

    @Test
    void failsClosedWhenCredentialsAreNotConfigured() {
        EsoProperties properties = new EsoProperties();
        properties.setBaseUrl("http://localhost:1");

        EsoClient client = new EsoClient(properties, RestClient.builder());

        assertThatThrownBy(() -> client.fetchConsumption("CUSTOMER1", "LT1234", true, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DataHubNotConfiguredException.class);
    }

    @Test
    void propagatesUpstreamServerErrorRatherThanSwallowingIt() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.respondJson("/oauth/token", 200, "{\"accessToken\":\"tok-456\"}");
            endpoint.respondJson("/api/v1/consumption", 502, "{\"error\":\"eso unavailable\"}");

            EsoProperties properties = new EsoProperties();
            properties.setBaseUrl(endpoint.baseUrl());
            properties.setClientId("client");
            properties.setClientSecret("secret");

            EsoClient client = new EsoClient(properties, RestClient.builder());

            assertThatThrownBy(() -> client.fetchConsumption("CUSTOMER1", "LT1234", true,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)))
                    .isInstanceOf(RestClientResponseException.class);
        }
    }
}
