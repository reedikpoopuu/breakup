package com.example.demo.registry;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit coverage of {@link AriregisterEsindusClient}'s XML request/response handling, no network. */
class AriregisterEsindusClientXmlTest {

    @Test
    void buildsARequestWithCredentialsAndRegistryCode() {
        String xml = AriregisterEsindusClient.buildEsindusRequest("api-user", "api-pass", "12345678");

        assertThat(xml).contains("<ariregister_kasutajanimi>api-user</ariregister_kasutajanimi>");
        assertThat(xml).contains("<ariregister_parool>api-pass</ariregister_parool>");
        assertThat(xml).contains("<ariregistri_kood>12345678</ariregistri_kood>");
        assertThat(xml).contains("esindus_v1");
    }

    private static final String RESPONSE_XML = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <esindus_v1Response xmlns="http://arireg.x-road.eu/producer/">
                  <keha>
                    <ettevotjad>
                      <item>
                        <ariregistri_kood>12345678</ariregistri_kood>
                        <arinimi>Nordic Woods OU</arinimi>
                        <isikud>
                          <item>
                            <isiku_liik>F</isiku_liik>
                            <fyysilise_isiku_eesnimi>Jaan</fyysilise_isiku_eesnimi>
                            <fyysilise_isiku_perenimi>Tamm</fyysilise_isiku_perenimi>
                            <fyysilise_isiku_kood>38001085718</fyysilise_isiku_kood>
                            <isikukood_riik>EST</isikukood_riik>
                            <fyysilise_isiku_synniaeg>1980-01-08</fyysilise_isiku_synniaeg>
                            <fyysilise_isiku_roll>PROC</fyysilise_isiku_roll>
                            <fyysilise_isiku_roll_tekstina>Procurator</fyysilise_isiku_roll_tekstina>
                            <ainuesindusoigus_olemas>true</ainuesindusoigus_olemas>
                          </item>
                          <item>
                            <isiku_liik>F</isiku_liik>
                            <fyysilise_isiku_eesnimi>Mari</fyysilise_isiku_eesnimi>
                            <fyysilise_isiku_perenimi>Mets</fyysilise_isiku_perenimi>
                            <fyysilise_isiku_kood>48505155716</fyysilise_isiku_kood>
                            <isikukood_riik>EST</isikukood_riik>
                            <fyysilise_isiku_roll>BOARD</fyysilise_isiku_roll>
                            <fyysilise_isiku_roll_tekstina>Board member</fyysilise_isiku_roll_tekstina>
                            <ainuesindusoigus_olemas>false</ainuesindusoigus_olemas>
                          </item>
                        </isikud>
                      </item>
                    </ettevotjad>
                  </keha>
                </esindus_v1Response>
              </soap:Body>
            </soap:Envelope>
            """;

    @Test
    void parsesEveryPersonAcrossAllMatchedCompanies() {
        List<CompanyRepresentative> representatives = AriregisterEsindusClient.parseEsindusResponse(RESPONSE_XML);

        assertThat(representatives).hasSize(2);
        CompanyRepresentative procurator = representatives.get(0);
        assertThat(procurator.givenName()).isEqualTo("Jaan");
        assertThat(procurator.surname()).isEqualTo("Tamm");
        assertThat(procurator.personalIdCode()).isEqualTo("38001085718");
        assertThat(procurator.personalIdCountry()).isEqualTo("EST");
        assertThat(procurator.dateOfBirth()).isEqualTo(LocalDate.of(1980, 1, 8));
        assertThat(procurator.role()).isEqualTo("PROC");
        assertThat(procurator.roleText()).isEqualTo("Procurator");
        assertThat(procurator.exclusiveRightOfRepresentation()).isTrue();

        CompanyRepresentative boardMember = representatives.get(1);
        assertThat(boardMember.personalIdCode()).isEqualTo("48505155716");
        assertThat(boardMember.exclusiveRightOfRepresentation()).isFalse();
        assertThat(boardMember.dateOfBirth()).isNull();
    }

    @Test
    void doesNotConfuseCompanyLevelAndPersonLevelItemElements() {
        // Regression guard: both nesting levels use the local name "item" - a naive
        // getElementsByTagNameNS("*", "item") over the whole document would merge them.
        String xml = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <esindus_v1Response xmlns="http://arireg.x-road.eu/producer/">
                      <keha>
                        <ettevotjad>
                          <item>
                            <ariregistri_kood>1</ariregistri_kood>
                            <isikud>
                              <item>
                                <fyysilise_isiku_kood>111</fyysilise_isiku_kood>
                                <isikukood_riik>EST</isikukood_riik>
                              </item>
                            </isikud>
                          </item>
                          <item>
                            <ariregistri_kood>2</ariregistri_kood>
                            <isikud>
                              <item>
                                <fyysilise_isiku_kood>222</fyysilise_isiku_kood>
                                <isikukood_riik>EST</isikukood_riik>
                              </item>
                            </isikud>
                          </item>
                        </ettevotjad>
                      </keha>
                    </esindus_v1Response>
                  </soap:Body>
                </soap:Envelope>
                """;

        List<CompanyRepresentative> representatives = AriregisterEsindusClient.parseEsindusResponse(xml);

        assertThat(representatives).extracting(CompanyRepresentative::personalIdCode).containsExactly("111", "222");
    }

    @Test
    void returnsEmptyListWhenNoCompanyIsMatched() {
        String xml = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <esindus_v1Response xmlns="http://arireg.x-road.eu/producer/">
                      <keha>
                        <ettevotjad/>
                      </keha>
                    </esindus_v1Response>
                  </soap:Body>
                </soap:Envelope>
                """;

        assertThat(AriregisterEsindusClient.parseEsindusResponse(xml)).isEmpty();
    }

    @Test
    void refusesToResolveExternalEntitiesInAnXxePayload() {
        // A DOCTYPE declaring an external entity that would read /etc/passwd if resolved.
        // A malicious/spoofed/MITM'd registry response is untrusted input just like any
        // other - this must fail closed (no DOCTYPE allowed at all) rather than leak file
        // contents into a parsed field.
        String xxePayload = """
                <?xml version="1.0"?>
                <!DOCTYPE root [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <esindus_v1Response xmlns="http://arireg.x-road.eu/producer/">
                      <keha>
                        <ettevotjad>
                          <item>
                            <isikud>
                              <item>
                                <fyysilise_isiku_kood>&xxe;</fyysilise_isiku_kood>
                              </item>
                            </isikud>
                          </item>
                        </ettevotjad>
                      </keha>
                    </esindus_v1Response>
                  </soap:Body>
                </soap:Envelope>
                """;

        assertThatThrownBy(() -> AriregisterEsindusClient.parseEsindusResponse(xxePayload))
                .as("parsing must fail rather than silently succeed with a resolved entity")
                .isInstanceOf(IllegalStateException.class);
    }
}
