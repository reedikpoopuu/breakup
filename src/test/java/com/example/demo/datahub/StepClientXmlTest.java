package com.example.demo.datahub;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit coverage of {@link StepClient}'s XML request/response handling, no network. */
class StepClientXmlTest {

    @Test
    void buildsARequestWithAllConfirmedGetObjectConsumptionFields() {
        String xml = StepClient.buildGetObjectConsumptionRequest(
                "CUSTOMER1", "LV1234", true, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

        assertThat(xml).contains("<customerEIC>CUSTOMER1</customerEIC>");
        assertThat(xml).contains("<objectEIC>LV1234</objectEIC>");
        assertThat(xml).contains("<customerPermission>true</customerPermission>");
        assertThat(xml).contains("<dateFrom>2026-01-01</dateFrom>");
        assertThat(xml).contains("<dateTo>2026-02-01</dateTo>");
        assertThat(xml).contains("<registerType>A+</registerType>");
        assertThat(xml).contains("<messageIc>");
    }

    @Test
    void omitsDateFromAndDateToWhenNotProvided() {
        String xml = StepClient.buildGetObjectConsumptionRequest("CUSTOMER1", "LV1234", false, null, null);

        assertThat(xml).doesNotContain("<dateFrom>");
        assertThat(xml).doesNotContain("<dateTo>");
        assertThat(xml).contains("<customerPermission>false</customerPermission>");
    }

    @Test
    void parsesRepeatingConsInfoEntriesIntoConsumptionRecords() {
        String xml = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <GetObjectConsumptionResponse xmlns="http://step.sadalestikls.lv/stdh">
                      <customerEIC>CUSTOMER1</customerEIC>
                      <objectEIC>LV1234</objectEIC>
                      <registerType>A+</registerType>
                      <averageYearlyConsumption>1200.5</averageYearlyConsumption>
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

        List<ConsumptionRecord> records = StepClient.parseGetObjectConsumptionResponse(xml);

        assertThat(records).hasSize(2);
        assertThat(records.get(0).getKwh()).isEqualByComparingTo(new BigDecimal("45.2"));
        assertThat(records.get(0).getGranularity()).isEqualTo(Granularity.HOURLY);
        assertThat(records.get(0).getSource()).isEqualTo(DataHubSource.STEP);
        assertThat(records.get(1).getKwh()).isEqualByComparingTo(new BigDecimal("50.1"));
    }

    @Test
    void returnsEmptyListWhenThereAreNoConsInfoEntries() {
        String xml = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <GetObjectConsumptionResponse xmlns="http://step.sadalestikls.lv/stdh">
                      <customerEIC>CUSTOMER1</customerEIC>
                      <objectEIC>LV1234</objectEIC>
                      <registerType>A+</registerType>
                    </GetObjectConsumptionResponse>
                  </soap:Body>
                </soap:Envelope>
                """;

        assertThat(StepClient.parseGetObjectConsumptionResponse(xml)).isEmpty();
    }

    @Test
    void buildsALoginRequestAndExtractsTheTokenFromTheResponse() {
        String requestXml = StepClient.buildLoginRequest("system-user", "s3cret");
        assertThat(requestXml).contains("<username>system-user</username>");
        assertThat(requestXml).contains("<password>s3cret</password>");

        String responseXml = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <LoginResponse xmlns="http://step.sadalestikls.lv/stdh">
                      <token>jwt-token-abc</token>
                    </LoginResponse>
                  </soap:Body>
                </soap:Envelope>
                """;
        assertThat(StepClient.extractLoginToken(responseXml)).isEqualTo("jwt-token-abc");
    }

    @Test
    void extractLoginTokenReturnsNullWhenNoTokenElementIsPresent() {
        assertThat(StepClient.extractLoginToken("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body/></soap:Envelope>"))
                .isNull();
    }
}
