package com.example.demo.pricing.scrape;

import com.example.demo.datahub.support.MockHttpEndpoint;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestClientSupplierPageFetcherTest {

    private final RestClientSupplierPageFetcher fetcher = new RestClientSupplierPageFetcher();

    @Test
    void fetchesTheResponseBodyWithABrowserUserAgent() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.handle("/price-page", exchange -> {
                assertThat(exchange.getRequestHeaders().getFirst("User-Agent")).contains("Mozilla");
                byte[] body = "<html><body>hello</body></html>".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });

            String html = fetcher.fetch(endpoint.baseUrl() + "/price-page");

            assertThat(html).contains("hello");
        }
    }

    @Test
    void wrapsNonSuccessResponsesInSupplierPageFetchException() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.handle("/blocked", exchange -> {
                exchange.sendResponseHeaders(403, -1);
                exchange.close();
            });

            assertThatThrownBy(() -> fetcher.fetch(endpoint.baseUrl() + "/blocked"))
                    .isInstanceOf(SupplierPageFetchException.class);
        }
    }
}
