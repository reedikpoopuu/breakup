package com.example.demo.registry;

import com.example.demo.common.CountryCode;
import com.example.demo.datahub.support.MockHttpEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises {@link AriregisterEsindusClient} end-to-end against a local mock HTTP server. */
class AriregisterEsindusClientMockEndpointTest {

    private static AriregisterProperties properties(String baseUrl) {
        AriregisterProperties properties = new AriregisterProperties();
        properties.setBaseUrl(baseUrl);
        properties.setUsername("api-user");
        properties.setPassword("api-pass");
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
    void fetchesAndMapsRepresentativesForARegistryCode() {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            endpoint.handle("/", exchange -> {
                String requestXml = readBody(exchange);
                assertThat(requestXml).contains("<ariregister_kasutajanimi>api-user</ariregister_kasutajanimi>");
                assertThat(requestXml).contains("<ariregistri_kood>12345678</ariregistri_kood>");

                String responseXml = """
                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                          <soap:Body>
                            <esindus_v1Response xmlns="http://arireg.x-road.eu/producer/">
                              <keha>
                                <ettevotjad>
                                  <item>
                                    <ariregistri_kood>12345678</ariregistri_kood>
                                    <isikud>
                                      <item>
                                        <fyysilise_isiku_eesnimi>Jaan</fyysilise_isiku_eesnimi>
                                        <fyysilise_isiku_perenimi>Tamm</fyysilise_isiku_perenimi>
                                        <fyysilise_isiku_kood>38001085718</fyysilise_isiku_kood>
                                        <isikukood_riik>EST</isikukood_riik>
                                        <fyysilise_isiku_roll>BOARD</fyysilise_isiku_roll>
                                        <fyysilise_isiku_roll_tekstina>Board member</fyysilise_isiku_roll_tekstina>
                                        <ainuesindusoigus_olemas>true</ainuesindusoigus_olemas>
                                      </item>
                                    </isikud>
                                  </item>
                                </ettevotjad>
                              </keha>
                            </esindus_v1Response>
                          </soap:Body>
                        </soap:Envelope>
                        """;
                respond(exchange, 200, responseXml);
            });

            AriregisterEsindusClient client = new AriregisterEsindusClient(properties(endpoint.baseUrl()), RestClient.builder());

            List<CompanyRepresentative> representatives = client.fetchRepresentatives("12345678");

            assertThat(representatives).hasSize(1);
            assertThat(representatives.get(0).personalIdCode()).isEqualTo("38001085718");
            assertThat(representatives.get(0).personalIdCountry()).isEqualTo("EST");
            assertThat(representatives.get(0).exclusiveRightOfRepresentation()).isTrue();
            assertThat(client.getCountry()).isEqualTo(CountryCode.EE);
        }
    }

    @Test
    void failsClosedWhenNotConfigured() {
        AriregisterEsindusClient client = new AriregisterEsindusClient(new AriregisterProperties(), RestClient.builder());

        assertThatThrownBy(() -> client.fetchRepresentatives("12345678"))
                .isInstanceOf(RegistryNotConfiguredException.class);
    }
}
