package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import com.example.demo.datahub.support.MockHttpEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StepClientMockEndpointTest {

    @Test
    void fetchesAndMapsConsumptionUsingApiKeyHeader() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            AtomicReference<String> seenApiKeyHeader = new AtomicReference<>();
            endpoint.handle("/api/v1/consumption", exchange -> {
                seenApiKeyHeader.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
                byte[] body = """
                        {"readings":[
                          {"start":"2026-01-01T00:00:00Z","end":"2026-02-01T00:00:00Z","kwh":980.0}
                        ]}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            StepProperties properties = new StepProperties();
            properties.setBaseUrl(endpoint.baseUrl());
            properties.setApiKey("api-key-123");

            StepClient client = new StepClient(properties, RestClient.builder());

            List<ConsumptionRecord> records = client.fetchConsumption("LV1234", LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 2, 1));

            assertThat(records).hasSize(1);
            assertThat(records.get(0).getKwh()).isEqualByComparingTo(new BigDecimal("980.0"));
            assertThat(records.get(0).getSource()).isEqualTo(DataHubSource.STEP);
            assertThat(seenApiKeyHeader.get()).isEqualTo("api-key-123");
            assertThat(client.getCountry()).isEqualTo(CountryCode.LV);
        }
    }

    @Test
    void failsClosedWhenApiKeyIsNotConfigured() {
        StepProperties properties = new StepProperties();
        properties.setBaseUrl("http://localhost:1");

        StepClient client = new StepClient(properties, RestClient.builder());

        assertThatThrownBy(() -> client.fetchConsumption("LV1234", LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DataHubNotConfiguredException.class);
    }

    @Test
    void propagatesUpstreamServerErrorRatherThanSwallowingIt() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.respondJson("/api/v1/consumption", 500, "{\"error\":\"step unavailable\"}");

            StepProperties properties = new StepProperties();
            properties.setBaseUrl(endpoint.baseUrl());
            properties.setApiKey("api-key-123");

            StepClient client = new StepClient(properties, RestClient.builder());

            assertThatThrownBy(() -> client.fetchConsumption("LV1234", LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 2, 1)))
                    .isInstanceOf(RestClientResponseException.class);
        }
    }
}
