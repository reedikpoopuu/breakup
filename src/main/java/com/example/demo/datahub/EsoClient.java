package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ESO Datahub (LT) adapter, rebuilt 2026-08 against the real spec: "DH API
 * documentation for independent supplier" v1.0.28 (datahub.eso.lt/sites/default/files/
 * 2026-06/DH%20API%20documentation%20for%20independent%20supplier%20v1.0.28_20260626.pdf).
 * The previous version assumed a synchronous REST/JSON call behind an OAuth2
 * client-credentials grant, matching Estfeed's shape - wrong on both counts:
 * <p>
 * <b>Auth (confirmed):</b> a single static, long-lived Bearer JWT issued directly by
 * ESO's DSO team by e-mail - {@code Authorization: Bearer <token>} on every call, no
 * token endpoint, no client_id/secret, no login step at all (spec section 5,
 * "Suppliers' digital certificates"). Simpler than Estfeed/STEP, not more complex.
 * <p>
 * <b>Data retrieval (confirmed) is a three-step async "order" pattern</b>, not a
 * single request (spec section 6.2 ASYNC / 7.3.3 / 7.3.5):
 * <ol>
 *   <li>{@code POST /gateway/order/v2/data-hr-15min-obj-lvl} with {@code dateFrom},
 *       {@code dateTo}, {@code consumptionCategories} (fixed to {@code ["P+"]} - active
 *       consumption, the spec's other values P-/Q-/Q+ are production/reactive power
 *       this app never needs), {@code objectNumbers}, {@code interval} (fixed to
 *       {@code "HOUR"}) - returns {@code {"orderId": ...}}.
 *   <li>{@code POST /gateway/order/v2/list} with {@code {"orderId": ...}}, polled until
 *       its {@code latestStatus} is {@code "IV"} (done) rather than {@code "P"}/{@code "V"}
 *       (still processing) or {@code "K"} (failed - the spec says not to retry client-side,
 *       ESO's own retry policy runs for up to 25h).
 *   <li>{@code GET /gateway/order/{orderId}/data-hr-15min-obj-lvl} for the actual
 *       consumption records.
 * </ol>
 * The spec explicitly allows this to take "several minutes" for a large real order and
 * says the wait/attempt budget is the client's call - the bounded poll here (a few
 * minutes max) suits an interactive/admin-triggered call; ARCH_SPEC.md already
 * anticipates a proper async fetch job as the eventual real caller of these adapters,
 * which is where a longer budget belongs.
 * <p>
 * {@code consumptionTime}'s exact timestamp format isn't shown with a concrete example
 * in the spec (only "string (dateTime)") - parsed here as an ISO-8601 instant, matching
 * every other Baltic DataHub seen so far; unconfirmed.
 * <p>
 * Consent has no per-request field in this API at all - a supplier's access to an
 * object is a separate, out-of-band "access right" resource (spec section 7.2), not
 * something passed on each order. {@code customerPermission=false} therefore refuses
 * client-side ({@link DataHubConsentRequiredException}) rather than silently proceeding
 * as if consent existed, since there's nowhere in the real request to tell the server it
 * doesn't.
 */
@Component
public class EsoClient implements DataHubClient {

    private static final String AUTHORIZATION = "Authorization";
    private static final Duration FIRST_WAIT = Duration.ofSeconds(2);
    private static final Duration REPEATING_WAIT = Duration.ofSeconds(3);
    private static final int MAX_POLL_ATTEMPTS = 20;
    private static final String STATUS_DONE = "IV";
    private static final String STATUS_FAILED = "K";
    private static final String NO_DATA_ERROR_CODE = "\"code\":2018";

    private record DataHrRequest(String dateFrom, String dateTo, List<String> consumptionCategories,
                                  List<String> objectNumbers, String interval) {
    }

    private record OrderIdRequest(Long orderId) {
    }

    private record OrderIdResponse(Long orderId) {
    }

    private record OrderStatusEntry(Long orderId, String latestStatus) {
    }

    private record ConsumptionEntry(String consumptionTime, BigDecimal amount) {
    }

    private record ConsumptionCategoryGroup(String consumptionCategory, List<ConsumptionEntry> consumptions) {
    }

    private record ObjectData(String objectNumber, List<ConsumptionCategoryGroup> consumptionCategories) {
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
    public List<ConsumptionRecord> fetchConsumption(String customerEic, String objectEic, boolean customerPermission,
                                                      LocalDate from, LocalDate to) {
        if (!properties.isConfigured()) {
            throw new DataHubNotConfiguredException(CountryCode.LT);
        }
        if (!customerPermission) {
            throw new DataHubConsentRequiredException(CountryCode.LT);
        }
        Long orderId = submitOrder(objectEic, from, to);
        awaitCompletion(orderId);
        return fetchOrderData(orderId);
    }

    private Long submitOrder(String objectEic, LocalDate from, LocalDate to) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        DataHrRequest request = new DataHrRequest(from.toString(), to.toString(), List.of("P+"),
                List.of(objectEic), "HOUR");
        OrderIdResponse response = restClient.post()
                .uri("/gateway/order/v2/data-hr-15min-obj-lvl")
                .header(AUTHORIZATION, "Bearer " + properties.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OrderIdResponse.class);
        if (response == null || response.orderId() == null) {
            throw new IllegalStateException("ESO did not return an orderId for the submitted order");
        }
        return response.orderId();
    }

    private void awaitCompletion(Long orderId) {
        sleep(FIRST_WAIT);
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            String status = fetchLatestStatus(orderId);
            if (STATUS_DONE.equals(status)) {
                return;
            }
            if (STATUS_FAILED.equals(status)) {
                throw new IllegalStateException("ESO order " + orderId + " failed (status K)");
            }
            sleep(REPEATING_WAIT);
        }
        throw new IllegalStateException("ESO order " + orderId + " did not complete within the poll budget");
    }

    private String fetchLatestStatus(Long orderId) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        List<OrderStatusEntry> statuses = restClient.post()
                .uri("/gateway/order/v2/list")
                .header(AUTHORIZATION, "Bearer " + properties.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new OrderIdRequest(orderId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<OrderStatusEntry>>() {
                });
        if (statuses == null || statuses.isEmpty()) {
            throw new IllegalStateException("ESO returned no status for order " + orderId);
        }
        return statuses.get(0).latestStatus();
    }

    private List<ConsumptionRecord> fetchOrderData(Long orderId) {
        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        List<ObjectData> data;
        try {
            data = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/gateway/order/{orderId}/data-hr-15min-obj-lvl")
                            .queryParam("first", 0)
                            .queryParam("count", 10000)
                            .build(orderId))
                    .header(AUTHORIZATION, "Bearer " + properties.getToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ObjectData>>() {
                    });
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 400 && e.getResponseBodyAsString().contains(NO_DATA_ERROR_CODE)) {
                return List.of();
            }
            throw e;
        }
        if (data == null) {
            return List.of();
        }
        List<ConsumptionRecord> records = new ArrayList<>();
        for (ObjectData object : data) {
            if (object.consumptionCategories() == null) {
                continue;
            }
            for (ConsumptionCategoryGroup group : object.consumptionCategories()) {
                if (!"P+".equals(group.consumptionCategory()) || group.consumptions() == null) {
                    continue;
                }
                for (ConsumptionEntry entry : group.consumptions()) {
                    Instant start = Instant.parse(entry.consumptionTime());
                    records.add(new ConsumptionRecord(
                            null,
                            start,
                            start.plus(Duration.ofHours(1)),
                            entry.amount(),
                            Granularity.HOURLY,
                            DataHubSource.ESO));
                }
            }
        }
        return records;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for ESO order to complete", e);
        }
    }
}
