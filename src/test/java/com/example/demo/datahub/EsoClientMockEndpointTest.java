package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import com.example.demo.datahub.support.MockHttpEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link EsoClient} against the confirmed real spec: a single static Bearer
 * token (no login step) and the three-step submit/poll/fetch order flow - see
 * EsoClient's javadoc for exactly what's confirmed vs. assumed.
 */
class EsoClientMockEndpointTest {

    private static EsoProperties properties(String baseUrl) {
        EsoProperties properties = new EsoProperties();
        properties.setBaseUrl(baseUrl);
        properties.setToken("dso-issued-token");
        return properties;
    }

    private static void respondJson(com.sun.net.httpserver.HttpExchange exchange, int status, String json) throws java.io.IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static String readBody(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        exchange.getRequestBody().transferTo(buffer);
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void fetchesAndMapsConsumptionThroughTheSubmitPollFetchOrderFlow() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.handle("/gateway/order/v2/data-hr-15min-obj-lvl", exchange -> {
                assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer dso-issued-token");
                String body = readBody(exchange);
                assertThat(body).contains("\"objectNumbers\":[\"LT1234\"]");
                assertThat(body).contains("\"consumptionCategories\":[\"P+\"]");
                assertThat(body).contains("\"interval\":\"HOUR\"");
                respondJson(exchange, 201, "{\"orderId\":10000001}");
            });
            endpoint.handle("/gateway/order/v2/list", exchange -> {
                assertThat(readBody(exchange)).contains("\"orderId\":10000001");
                respondJson(exchange, 200, "[{\"orderId\":10000001,\"latestStatus\":\"IV\"}]");
            });
            endpoint.handle("/gateway/order/10000001/data-hr-15min-obj-lvl", exchange -> {
                assertThat(exchange.getRequestURI().getQuery()).contains("first=0").contains("count=10000");
                respondJson(exchange, 200, """
                        [
                          {
                            "objectNumber": "LT1234",
                            "consumptionCategories": [
                              {
                                "consumptionCategory": "P+",
                                "consumptions": [
                                  {"consumptionTime": "2026-01-01T00:00:00Z", "amount": 12.5, "valueType": "VAL"},
                                  {"consumptionTime": "2026-01-01T01:00:00Z", "amount": 13.1, "valueType": "VAL"}
                                ]
                              }
                            ]
                          }
                        ]
                        """);
            });

            EsoClient client = new EsoClient(properties(endpoint.baseUrl()), RestClient.builder());

            List<ConsumptionRecord> records = client.fetchConsumption("CUSTOMER1", "LT1234", true,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

            assertThat(records).hasSize(2);
            assertThat(records.get(0).getKwh()).isEqualByComparingTo(new BigDecimal("12.5"));
            assertThat(records.get(0).getIntervalStart().toString()).isEqualTo("2026-01-01T00:00:00Z");
            assertThat(records.get(0).getIntervalEnd().toString()).isEqualTo("2026-01-01T01:00:00Z");
            assertThat(records.get(0).getGranularity()).isEqualTo(Granularity.HOURLY);
            assertThat(records.get(0).getSource()).isEqualTo(DataHubSource.ESO);
            assertThat(client.getCountry()).isEqualTo(CountryCode.LT);
        }
    }

    @Test
    void failsClosedWhenTokenIsNotConfigured() {
        EsoProperties properties = new EsoProperties();
        properties.setBaseUrl("http://localhost:1");

        EsoClient client = new EsoClient(properties, RestClient.builder());

        assertThatThrownBy(() -> client.fetchConsumption("CUSTOMER1", "LT1234", true, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DataHubNotConfiguredException.class);
    }

    @Test
    void refusesToCallWithoutCustomerPermissionSinceTheRealApiHasNoConsentField() {
        EsoClient client = new EsoClient(properties("http://localhost:1"), RestClient.builder());

        assertThatThrownBy(() -> client.fetchConsumption("CUSTOMER1", "LT1234", false, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DataHubConsentRequiredException.class);
    }

    @Test
    void propagatesUpstreamServerErrorRatherThanSwallowingIt() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.respondJson("/gateway/order/v2/data-hr-15min-obj-lvl", 502, "{\"error\":\"eso unavailable\"}");

            EsoClient client = new EsoClient(properties(endpoint.baseUrl()), RestClient.builder());

            assertThatThrownBy(() -> client.fetchConsumption("CUSTOMER1", "LT1234", true,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)))
                    .isInstanceOf(RestClientResponseException.class);
        }
    }

    @Test
    void returnsEmptyListWhenTheOrderHasNoData() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.respondJson("/gateway/order/v2/data-hr-15min-obj-lvl", 201, "{\"orderId\":10000002}");
            endpoint.respondJson("/gateway/order/v2/list", 200, "[{\"orderId\":10000002,\"latestStatus\":\"IV\"}]");
            endpoint.respondJson("/gateway/order/10000002/data-hr-15min-obj-lvl", 400,
                    "{\"code\":2018,\"text\":\"There is no data for the selected search parameters, the response is empty.\"}");

            EsoClient client = new EsoClient(properties(endpoint.baseUrl()), RestClient.builder());

            List<ConsumptionRecord> records = client.fetchConsumption("CUSTOMER1", "LT1234", true,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

            assertThat(records).isEmpty();
        }
    }

    @Test
    void throwsWhenTheOrderEndsInFailedStatus() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.respondJson("/gateway/order/v2/data-hr-15min-obj-lvl", 201, "{\"orderId\":10000003}");
            endpoint.respondJson("/gateway/order/v2/list", 200, "[{\"orderId\":10000003,\"latestStatus\":\"K\"}]");

            EsoClient client = new EsoClient(properties(endpoint.baseUrl()), RestClient.builder());

            assertThatThrownBy(() -> client.fetchConsumption("CUSTOMER1", "LT1234", true,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("failed");
        }
    }

    @Test
    void pollsUntilStatusBecomesDone() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            AtomicInteger statusCalls = new AtomicInteger();
            endpoint.respondJson("/gateway/order/v2/data-hr-15min-obj-lvl", 201, "{\"orderId\":10000004}");
            endpoint.handle("/gateway/order/v2/list", exchange -> {
                boolean firstCall = statusCalls.getAndIncrement() == 0;
                respondJson(exchange, 200, "[{\"orderId\":10000004,\"latestStatus\":\"" + (firstCall ? "V" : "IV") + "\"}]");
            });
            endpoint.respondJson("/gateway/order/10000004/data-hr-15min-obj-lvl", 200, "[]");

            EsoClient client = new EsoClient(properties(endpoint.baseUrl()), RestClient.builder());

            List<ConsumptionRecord> records = client.fetchConsumption("CUSTOMER1", "LT1234", true,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

            assertThat(records).isEmpty();
            assertThat(statusCalls.get()).isEqualTo(2);
        }
    }
}
