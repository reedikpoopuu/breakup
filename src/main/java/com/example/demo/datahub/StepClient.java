package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * STEP / Sadales tikls (LV) adapter - "Vienotais datu apmainas standarts v1.5". Uses an
 * API-key header pending confirmation of the exact auth model from the full standard
 * document (ARCH_SPEC.md section 3.2).
 */
@Component
public class StepClient implements DataHubClient {

    private record Interval(String start, String end, BigDecimal kwh) {
    }

    private record ConsumptionResponse(List<Interval> readings) {
    }

    private final StepProperties properties;
    private final RestClient.Builder restClientBuilder;

    public StepClient(StepProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public CountryCode getCountry() {
        return CountryCode.LV;
    }

    @Override
    public List<ConsumptionRecord> fetchConsumption(String eicCode, LocalDate from, LocalDate to) {
        if (!properties.isConfigured()) {
            throw new DataHubNotConfiguredException(CountryCode.LV);
        }
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();

        ConsumptionResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/consumption")
                        .queryParam("eic", eicCode)
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .header("X-Api-Key", properties.getApiKey())
                .retrieve()
                .body(ConsumptionResponse.class);

        if (response == null || response.readings() == null) {
            return List.of();
        }
        return response.readings().stream()
                .map(reading -> new ConsumptionRecord(
                        null,
                        java.time.Instant.parse(reading.start()),
                        java.time.Instant.parse(reading.end()),
                        reading.kwh(),
                        Granularity.MONTHLY,
                        DataHubSource.STEP))
                .toList();
    }
}
