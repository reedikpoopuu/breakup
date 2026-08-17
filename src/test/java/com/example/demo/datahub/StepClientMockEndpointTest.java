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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link StepClient} end-to-end against the confirmed GetObjectConsumption
 * contract (see StepClient's javadoc for the login handshake's separate, lower
 * confidence level - this test still fixtures a login response since fetchConsumption
 * calls it, but doesn't claim that fixture matches STEP's real wire format).
 */
class StepClientMockEndpointTest {

    private static final String LOGIN_RESPONSE_XML = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <LoginResponse xmlns="http://step.sadalestikls.lv/stdh">
                  <token>jwt-token-abc</token>
                </LoginResponse>
              </soap:Body>
            </soap:Envelope>
            """;

    private static StepProperties properties(String baseUrl) {
        StepProperties properties = new StepProperties();
        properties.setBaseUrl(baseUrl);
        properties.setAuthBaseUrl(baseUrl);
        properties.setUsername("system-user");
        properties.setPassword("secret");
        return properties;
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String xml) throws java.io.IOException {
        byte[] body = xml.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/xml");
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
    void fetchesAndMapsConsumptionThroughTheLoginThenGetObjectConsumptionFlow() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.handle("/Auth", exchange -> respond(exchange, 200, LOGIN_RESPONSE_XML));

            endpoint.handle("/MarketMessagesSupplier", exchange -> {
                assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer jwt-token-abc");
                String requestXml = readBody(exchange);
                assertThat(requestXml).contains("<customerEIC>CUSTOMER1</customerEIC>");
                assertThat(requestXml).contains("<objectEIC>LV1234</objectEIC>");
                assertThat(requestXml).contains("<customerPermission>true</customerPermission>");
                assertThat(requestXml).contains("<registerType>A+</registerType>");

                String responseXml = """
                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                          <soap:Body>
                            <GetObjectConsumptionResponse xmlns="http://step.sadalestikls.lv/stdh">
                              <customerEIC>CUSTOMER1</customerEIC>
                              <objectEIC>LV1234</objectEIC>
                              <registerType>A+</registerType>
                              <consInfo>
                                <consDT>2026-01-01T00:00:00Z</consDT>
                                <cons>45.2</cons>
                              </consInfo>
                              <consInfo>
                                <consDT>2026-01-01T01:00:00Z</consDT>
                                <cons>50.1</cons>
                              </consInfo>
                            </GetObjectConsumptionResponse>
                          </soap:Body>
                        </soap:Envelope>
                        """;
                respond(exchange, 200, responseXml);
            });

            StepClient client = new StepClient(properties(endpoint.baseUrl()), RestClient.builder());

            List<ConsumptionRecord> records = client.fetchConsumption("CUSTOMER1", "LV1234", true,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

            assertThat(records).hasSize(2);
            assertThat(records.get(0).getKwh()).isEqualByComparingTo(new BigDecimal("45.2"));
            assertThat(records.get(0).getIntervalStart().toString()).isEqualTo("2026-01-01T00:00:00Z");
            assertThat(records.get(0).getIntervalEnd().toString()).isEqualTo("2026-01-01T01:00:00Z");
            assertThat(records.get(0).getSource()).isEqualTo(DataHubSource.STEP);
            assertThat(client.getCountry()).isEqualTo(CountryCode.LV);
        }
    }

    @Test
    void failsClosedWhenNotFullyConfigured() {
        StepProperties properties = new StepProperties();
        properties.setBaseUrl("http://localhost:1");
        // username/password/authBaseUrl deliberately left unset

        StepClient client = new StepClient(properties, RestClient.builder());

        assertThatThrownBy(() -> client.fetchConsumption("CUSTOMER1", "LV1234", true, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DataHubNotConfiguredException.class);
    }

    @Test
    void propagatesUpstreamServerErrorRatherThanSwallowingIt() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.handle("/Auth", exchange -> respond(exchange, 200, LOGIN_RESPONSE_XML));
            endpoint.handle("/MarketMessagesSupplier", exchange -> respond(exchange, 500, "<soap:Envelope/>"));

            StepClient client = new StepClient(properties(endpoint.baseUrl()), RestClient.builder());

            assertThatThrownBy(() -> client.fetchConsumption("CUSTOMER1", "LV1234", true,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)))
                    .isInstanceOf(RestClientResponseException.class);
        }
    }
}
