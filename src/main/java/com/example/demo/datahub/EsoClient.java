package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ESO Datahub (LT) adapter - Sandbox API documentation v6.4. OAuth2-style
 * client-credentials token then a consumption query, same shape as Estfeed; the
 * sandbox environment is usable ahead of production credentials (ARCH_SPEC.md
 * section 3.2).
 */
@Component
public class EsoClient implements DataHubClient {

    private record TokenResponse(String accessToken) {
    }

    private record Interval(String start, String end, BigDecimal kwh) {
    }

    private record ConsumptionResponse(List<Interval> intervals) {
    }

    private final EsoProperties properties;
    private final RestClient.Builder restClientBuilder;

    public EsoClient(EsoProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public CountryCode getCountry() {
        return CountryCode.LT;
    }

    @Override
    public List<ConsumptionRecord> fetchConsumption(String eicCode, LocalDate from, LocalDate to) {
        if (!properties.isConfigured()) {
            throw new DataHubNotConfiguredException(CountryCode.LT);
        }
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        String accessToken = fetchAccessToken(restClient);

        ConsumptionResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/consumption")
                        .queryParam("eic", eicCode)
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(ConsumptionResponse.class);

        if (response == null || response.intervals() == null) {
            return List.of();
        }
        return response.intervals().stream()
                .map(interval -> new ConsumptionRecord(
                        null,
                        java.time.Instant.parse(interval.start()),
                        java.time.Instant.parse(interval.end()),
                        interval.kwh(),
                        Granularity.MONTHLY,
                        DataHubSource.ESO))
                .toList();
    }

    private String fetchAccessToken(RestClient restClient) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        TokenResponse token = restClient.post()
                .uri("/oauth/token")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (token == null || token.accessToken() == null) {
            throw new DataHubNotConfiguredException(CountryCode.LT);
        }
        return token.accessToken();
    }
}
