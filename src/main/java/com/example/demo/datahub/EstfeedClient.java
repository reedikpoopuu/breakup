package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Elering Estfeed (EE) adapter, verified 2026-08 against the real spec:
 * github.com/Elering/estfeed-datahub-docs ({@code eng/03-authentication-and-authorisation.md},
 * {@code eng/12-metering-data.md}) and the live OpenAPI document at
 * https://datahub.elering.ee/v3/api-docs. The previous version of this class was a
 * plausible-looking guess (see git history) that got the token endpoint, the data
 * endpoint path, the auth-context transport, and the token response field name all
 * wrong - none of it would have worked against the real service.
 * <p>
 * Auth: Keycloak OpenID Connect client-credentials grant at
 * {@code {authBaseUrl}/realms/{authRealm}/protocol/openid-connect/token} (test realm
 * "estfeed-public" on test-kc.elering.ee per the docs), with {@code scope=openid}
 * required alongside the usual client_id/client_secret. The token response is a
 * standard OAuth2 JSON body (RFC 6749: snake_case {@code access_token}) - deserialized
 * here via {@code @JsonProperty} since Jackson won't match that to a camelCase record
 * component automatically (the previous version silently got a null token from this).
 * <p>
 * Data: {@code GET /api/v2/metering-data/electricity}. The "marketParticipantContext"
 * the prose docs describe conceptually is carried as request headers on this deployed
 * v2 endpoint, not a JSON body: {@code x-market-participant-eic},
 * {@code x-market-participant-role}, {@code x-commodity-type}, plus a required
 * {@code x-document-identification} UUID per request. Query parameters are
 * {@code meteringPointEics} (plural - an array) and {@code periodStart}/{@code periodEnd}
 * (not {@code from}/{@code to}).
 * <p>
 * Residual uncertainty: the live OpenAPI document truncated before the full nested
 * schema for each {@code periods} entry, so only {@code time}/{@code quantity} are
 * mapped here - confirmed present, but there may be more fields (e.g. a reading-type
 * discriminator) not yet accounted for. The optional {@code resolution} query
 * parameter is deliberately left unset rather than guessing at its enum values -
 * electricity metering data is documented as natively 15-minute resolution, which is
 * what's assumed for the interval end computed below.
 */
@Component
public class EstfeedClient implements DataHubClient {

    private static final Duration NATIVE_INTERVAL = Duration.ofMinutes(15);

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    private record MeteringPeriod(Instant time, BigDecimal quantity) {
    }

    private record ElectricityMeteringData(String meterEic, List<MeteringPeriod> periods) {
    }

    private record BulkResponse(List<ElectricityMeteringData> data) {
    }

    private final EstfeedProperties properties;
    private final RestClient.Builder restClientBuilder;

    public EstfeedClient(EstfeedProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public CountryCode getCountry() {
        return CountryCode.EE;
    }

    @Override
    public List<ConsumptionRecord> fetchConsumption(String eicCode, LocalDate from, LocalDate to) {
        if (!properties.isConfigured()) {
            throw new DataHubNotConfiguredException(CountryCode.EE);
        }
        String accessToken = fetchAccessToken();

        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        BulkResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v2/metering-data/electricity")
                        .queryParam("meteringPointEics", eicCode)
                        .queryParam("periodStart", from)
                        .queryParam("periodEnd", to)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .header("x-document-identification", UUID.randomUUID().toString())
                .header("x-market-participant-eic", properties.getMarketParticipantEic())
                .header("x-market-participant-role", properties.getMarketParticipantRole())
                .header("x-commodity-type", "ELECTRICITY")
                .retrieve()
                .body(BulkResponse.class);

        if (response == null || response.data() == null) {
            return List.of();
        }
        return response.data().stream()
                .flatMap(meter -> meter.periods() == null ? List.<MeteringPeriod>of().stream() : meter.periods().stream())
                .map(period -> new ConsumptionRecord(
                        null,
                        period.time(),
                        period.time().plus(NATIVE_INTERVAL),
                        period.quantity(),
                        Granularity.QUARTER_HOURLY,
                        DataHubSource.ESTFEED))
                .toList();
    }

    private String fetchAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("scope", "openid");

        RestClient authClient = restClientBuilder.build();
        TokenResponse token = authClient.post()
                .uri(properties.getAuthBaseUrl() + "/realms/" + properties.getAuthRealm() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (token == null || token.accessToken() == null) {
            throw new DataHubNotConfiguredException(CountryCode.EE);
        }
        return token.accessToken();
    }
}
