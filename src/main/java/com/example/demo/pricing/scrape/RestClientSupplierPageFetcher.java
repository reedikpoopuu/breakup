package com.example.demo.pricing.scrape;

import com.example.demo.common.OutboundUrlValidator;
import com.example.demo.common.UnsafeOutboundUrlException;
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

    private final RestClient restClient;
    private final OutboundUrlValidator outboundUrlValidator;

    public RestClientSupplierPageFetcher(RestClient.Builder restClientBuilder, OutboundUrlValidator outboundUrlValidator) {
        // Built from the injected, Spring-managed builder (not a standalone RestClient.builder())
        // specifically so this picks up RestClientTimeoutConfig's connect/read timeouts -
        // this is the one outbound client in the app whose target URL isn't fixed
        // application config, it's admin-set, so it's also the one most worth timing out.
        this.restClient = restClientBuilder.build();
        this.outboundUrlValidator = outboundUrlValidator;
    }

    @Override
    public String fetch(String url) {
        // Re-validated here, not just at write time (SupplierService) - a hostname can
        // resolve to a public address when saved and an internal one by the time the
        // scraper actually runs (DNS rebinding), so the check that matters is the one
        // immediately before the request goes out. Wrapped as SupplierPageFetchException,
        // same as any other fetch failure, so one supplier failing this check doesn't
        // abort the whole scrape run - it's skipped and logged like any other bad fetch.
        try {
            outboundUrlValidator.validate(url);
        } catch (UnsafeOutboundUrlException e) {
            throw new SupplierPageFetchException(url, e);
        }
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
