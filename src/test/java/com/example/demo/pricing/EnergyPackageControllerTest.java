package com.example.demo.pricing;

import com.example.demo.auth.AppUser;
import com.example.demo.auth.AppUserRepository;
import com.example.demo.auth.Role;
import com.example.demo.auth.TokenService;
import com.example.demo.pricing.support.SupplierPageFetcherTestConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SupplierPageFetcherTestConfig.class)
class EnergyPackageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    TokenService tokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    String adminAuthHeader() {
        AppUser admin = appUserRepository.findBySmartIdIdentity("EE-38507030022")
                .orElseThrow(() -> new IllegalStateException("test admin not bootstrapped"));
        return "Bearer " + tokenService.issue(admin);
    }

    String userAuthHeader() {
        AppUser user = appUserRepository.save(new AppUser("LV-someone-" + System.nanoTime(), "Some User", Role.USER));
        return "Bearer " + tokenService.issue(user);
    }

    private static JsonNode byPackageName(JsonNode array, String packageName) {
        for (JsonNode node : array) {
            if (node.get("packageName").asText().equals(packageName)) {
                return node;
            }
        }
        throw new AssertionError("no package named '" + packageName + "' in " + array);
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/admin/packages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/packages").header("Authorization", userAuthHeader()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsSeededPackagesForAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/packages").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(9)))
                .andExpect(jsonPath("$[?(@.supplierName == 'Eesti Energia')]").exists())
                .andExpect(jsonPath("$[?(@.supplierName == 'Ignitis')]").exists());
    }

    @Test
    void filtersPackagesByCountry() throws Exception {
        mockMvc.perform(get("/api/admin/packages").param("country", "LT").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.country != 'LT')]").isEmpty());
    }

    @Test
    void scrapeRequiresAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/packages/scrape"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/packages/scrape").header("Authorization", userAuthHeader()))
                .andExpect(status().isForbidden());
    }

    @Test
    void scrapeAddsRealOffersFromRegisteredSuppliersAndLeavesManualPackagesUntouched() throws Exception {
        String beforeBody = mockMvc.perform(get("/api/admin/packages").header("Authorization", adminAuthHeader()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String manualTimestampBefore = byPackageName(objectMapper.readTree(beforeBody), "Eesti Energia Börs")
                .get("lastUpdated").asText();

        String afterBody = mockMvc.perform(post("/api/admin/packages/scrape").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode after = objectMapper.readTree(afterBody);

        JsonNode manualAfter = byPackageName(after, "Eesti Energia Börs");
        assertThat(manualAfter.get("lastUpdated").asText()).isEqualTo(manualTimestampBefore);
        assertThat(manualAfter.get("source").asText()).isEqualTo("MANUAL");

        JsonNode scraped = byPackageName(after, "KINDEL PAKETT");
        assertThat(scraped.get("source").asText()).isEqualTo("SCRAPED");
        assertThat(scraped.get("supplierName").asText()).isEqualTo("Elenger");
        assertThat(new BigDecimal(scraped.get("pricePerKwh").asText())).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void createRequiresAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualPackageJson("Requires Admin")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateAManualPackageForASupplierThatCannotBeScraped() throws Exception {
        String body = mockMvc.perform(post("/api/admin/packages")
                        .header("Authorization", adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualPackageJson("Negotiated Enefit Deal")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode created = objectMapper.readTree(body);
        assertThat(created.get("source").asText()).isEqualTo("MANUAL");
        assertThat(created.get("visible").asBoolean()).isTrue();
        assertThat(created.get("supplierName").asText()).isEqualTo("Enefit");
    }

    @Test
    void adminCanHideAPackageAndItDisappearsFromThePublicEndpoint() throws Exception {
        String body = mockMvc.perform(post("/api/admin/packages")
                        .header("Authorization", adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualPackageJson("Toggle Visibility Deal")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());

        mockMvc.perform(patch("/api/admin/packages/" + id + "/visibility")
                        .header("Authorization", adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible").value(false));

        mockMvc.perform(get("/api/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").doesNotExist());
    }

    @Test
    void adminCanDeleteAManualPackage() throws Exception {
        String body = mockMvc.perform(post("/api/admin/packages")
                        .header("Authorization", adminAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualPackageJson("Delete Me Deal")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(delete("/api/admin/packages/" + id).header("Authorization", adminAuthHeader()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/admin/packages/" + id).header("Authorization", adminAuthHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicPackagesEndpointNeedsNoAuth() throws Exception {
        mockMvc.perform(get("/api/packages"))
                .andExpect(status().isOk());
    }

    private static String manualPackageJson(String packageName) {
        return """
                {"packageName":"%s","supplierName":"Enefit","country":"EE","pricePerKwh":0.1500,"marginPerKwh":0.0200}
                """.formatted(packageName);
    }
}
