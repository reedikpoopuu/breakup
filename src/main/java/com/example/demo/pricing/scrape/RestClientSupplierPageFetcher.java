package com.example.demo.pricing.scrape;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Plain HTTP GET via Spring's {@link RestClient}. Several of these supplier sites sit
 * behind a WAF that 403/451s Java's default "Java/x.x" user agent - a normal browser
 * user agent is what got a 200 during manual verification, so it's set explicitly here
 * rather than left at the client default.
 */
@Component
public class RestClientSupplierPageFetcher implements SupplierPageFetcher {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    private final RestClient restClient = RestClient.builder().build();

    @Override
    public String fetch(String url) {
        try {
            return restClient.get()
                    .uri(url)
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new SupplierPageFetchException(url, e);
        }
    }
}
