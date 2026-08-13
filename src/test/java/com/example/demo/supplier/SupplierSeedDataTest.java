package com.example.demo.supplier;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Verifies the first-boot seed data ({@link SupplierSeeder}) matches the PM-supplied
 * source of truth in PM_ANSWERS.txt exactly - name, rfqEmail and priceUrl per country.
 * Exercised through the public read API (ARCH_SPEC.md section 1.3) so it also proves
 * the seeded rows are actually active and reachable, not just present in the DB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupplierSeedDataTest {

    @Autowired
    MockMvc mockMvc;

    private static Stream<Arguments> pmSuppliedSuppliers() {
        return Stream.of(
                Arguments.of("EE", "Enefit", "arikliendid@enefit.ee",
                        "https://www.enefit.ee/et/era/elekter/elektrileping-ja-paketid#/"),
                Arguments.of("EE", "Alexela", "ariklient@alexela.ee",
                        "https://www.alexela.ee/et/elekter"),
                Arguments.of("EE", "Elenger", "klienditugi@elenger.ee",
                        "https://elenger.ee/kodukliendile/elekter/"),
                Arguments.of("EE", "Elektrum", "myyk@elektrum.ee",
                        "https://www.elektrum.ee/ee/eraklient/elekter/elektripaketid"),
                Arguments.of("EE", "Sunly", "elekter@sunly.ee",
                        "https://sunly.ee/elekter/ari"),

                Arguments.of("LV", "Elektrum", "klientu.serviss@elektrum.lv",
                        "https://www.elektrum.lv/lv/majai/klientiem/elektribas-podukta-izvele/produkti/#produkti"),
                Arguments.of("LV", "Enefit", "bizness@enefit.lv",
                        "https://www.enefit.lv/lv/majai/elektriba#/"),
                Arguments.of("LV", "Virši", "info@virsi.lv",
                        "https://www.virsi.lv/lv/privatpersonam/elektriba/elektriba"),

                Arguments.of("LT", "Ignitis", "info@ignitis.lt",
                        "https://ignitis.lt/elektros-kainu-skaiciuokle/elektros-planai"),
                Arguments.of("LT", "Enefit", "energija@enefit.lt",
                        "https://www.enefit.lt/lt/privatiems/elektra#/"),
                Arguments.of("LT", "Elektrum", "info@elektrum.lt",
                        "https://www.elektrum.lt/lt/namams/elektra")
        );
    }

    @ParameterizedTest(name = "{0} supplier {1} has the PM-supplied email and price URL")
    @MethodSource("pmSuppliedSuppliers")
    void seededSupplierMatchesPmSuppliedContactDetails(String country, String name, String rfqEmail,
                                                         String priceUrl) throws Exception {
        String body = mockMvc.perform(get("/api/suppliers").param("country", country))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode suppliers = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);

        JsonNode match = null;
        for (JsonNode candidate : suppliers) {
            if (candidate.get("name").asText().equals(name)) {
                match = candidate;
                break;
            }
        }

        assertThat(match).as("supplier '%s' seeded for country %s", name, country).isNotNull();
        assertThat(match.get("rfqEmail").asText()).isEqualTo(rfqEmail);
        assertThat(match.get("priceUrl").asText()).isEqualTo(priceUrl);
        assertThat(match.get("active").asBoolean()).isTrue();
    }

    @Test
    void seedsExactlyElevenSuppliersAcrossTheThreeCountries() throws Exception {
        String body = mockMvc.perform(get("/api/suppliers"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode suppliers = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);

        long seeded = 0;
        for (JsonNode candidate : suppliers) {
            if (isPmSeeded(candidate.get("name").asText(), candidate.get("country").asText())) {
                seeded++;
            }
        }
        assertThat(seeded).isEqualTo(11);
    }

    private boolean isPmSeeded(String name, String country) {
        return pmSuppliedSuppliers().anyMatch(args -> args.get()[0].equals(country) && args.get()[1].equals(name));
    }
}
