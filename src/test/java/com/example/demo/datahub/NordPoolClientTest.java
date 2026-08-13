package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import com.example.demo.datahub.support.MockHttpEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class NordPoolClientTest {

    @Test
    void needsNoCredentialAndMapsPricesForRequestedCountry() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            AtomicReference<String> seenQuery = new AtomicReference<>();
            endpoint.handle("/api/v1/day-ahead-prices", exchange -> {
                seenQuery.set(exchange.getRequestURI().getQuery());
                byte[] body = """
                        {"entries":[
                          {"deliveryStart":"2026-01-01T00:00:00Z","eurPerMwh":54.32}
                        ]}
                        """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            NordPoolProperties properties = new NordPoolProperties();
            properties.setBaseUrl(endpoint.baseUrl());
            NordPoolClient client = new NordPoolClient(properties, RestClient.builder());

            List<SpotPrice> prices = client.fetchDayAheadPrices(CountryCode.EE, LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 1, 2));

            assertThat(prices).hasSize(1);
            assertThat(prices.get(0).getPriceEurPerMwh()).isEqualByComparingTo(new BigDecimal("54.32"));
            assertThat(prices.get(0).getCountry()).isEqualTo(CountryCode.EE);
            assertThat(seenQuery.get()).contains("deliveryArea=EE");
        }
    }

    @Test
    void returnsEmptyListWhenNoEntriesInResponse() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.respondJson("/api/v1/day-ahead-prices", 200, "{\"entries\":[]}");

            NordPoolProperties properties = new NordPoolProperties();
            properties.setBaseUrl(endpoint.baseUrl());
            NordPoolClient client = new NordPoolClient(properties, RestClient.builder());

            List<SpotPrice> prices = client.fetchDayAheadPrices(CountryCode.LV, LocalDate.now(), LocalDate.now());

            assertThat(prices).isEmpty();
        }
    }

    @Test
    void requestsEachCountrySeparately() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            AtomicReference<String> seenQuery = new AtomicReference<>();
            endpoint.handle("/api/v1/day-ahead-prices", exchange -> {
                seenQuery.set(exchange.getRequestURI().getQuery());
                byte[] body = "{\"entries\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            NordPoolProperties properties = new NordPoolProperties();
            properties.setBaseUrl(endpoint.baseUrl());
            NordPoolClient client = new NordPoolClient(properties, RestClient.builder());

            client.fetchDayAheadPrices(CountryCode.LT, LocalDate.now(), LocalDate.now());

            assertThat(seenQuery.get()).contains("deliveryArea=LT");
        }
    }
}
