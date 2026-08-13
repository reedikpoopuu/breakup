package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Nord Pool day-ahead spot price feed - data-api.nordpoolgroup.com. Not a per-company
 * DataHub adapter (no eicCode/representative-rights gate), so it does not implement
 * {@link DataHubClient}; needs no per-country credential and can be wired immediately
 * (ARCH_SPEC.md section 3.2/3.4).
 */
@Component
public class NordPoolClient {

    private record PricePoint(String deliveryStart, BigDecimal eurPerMwh) {
    }

    private record DayAheadResponse(List<PricePoint> entries) {
    }

    private final NordPoolProperties properties;
    private final RestClient.Builder restClientBuilder;

    public NordPoolClient(NordPoolProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    public List<SpotPrice> fetchDayAheadPrices(CountryCode country, LocalDate from, LocalDate to) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();

        DayAheadResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/day-ahead-prices")
                        .queryParam("market", "DayAhead")
                        .queryParam("deliveryArea", country.name())
                        .queryParam("currency", "EUR")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .retrieve()
                .body(DayAheadResponse.class);

        if (response == null || response.entries() == null) {
            return List.of();
        }
        return response.entries().stream()
                .map(entry -> new SpotPrice(country, java.time.Instant.parse(entry.deliveryStart()), entry.eurPerMwh()))
                .toList();
    }
}
