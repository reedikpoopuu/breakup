package com.example.demo.pricing;

import com.example.demo.auth.AppUser;
import com.example.demo.auth.AppUserRepository;
import com.example.demo.auth.Role;
import com.example.demo.auth.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnergyPackageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    TokenService tokenService;

    String adminAuthHeader() {
        AppUser admin = appUserRepository.findBySmartIdIdentity("EE-38507030022")
                .orElseThrow(() -> new IllegalStateException("test admin not bootstrapped"));
        return "Bearer " + tokenService.issue(admin);
    }

    String userAuthHeader() {
        AppUser user = appUserRepository.save(new AppUser("LV-someone-" + System.nanoTime(), "Some User", Role.USER));
        return "Bearer " + tokenService.issue(user);
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
    void scrapeUpdatesLastUpdatedTimestampAndKeepsPricesPositive() throws Exception {
        String beforeBody = mockMvc.perform(get("/api/admin/packages").header("Authorization", adminAuthHeader()))
                .andReturn().getResponse().getContentAsString();
        JsonNode before = new com.fasterxml.jackson.databind.ObjectMapper().readTree(beforeBody).get(0);
        String beforeTimestamp = before.get("lastUpdated").asText();

        String afterBody = mockMvc.perform(post("/api/admin/packages/scrape").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode afterList = new com.fasterxml.jackson.databind.ObjectMapper().readTree(afterBody);

        assertThat(afterList.size()).isGreaterThanOrEqualTo(9);
        for (JsonNode pkg : afterList) {
            assertThat(new BigDecimal(pkg.get("pricePerKwh").asText())).isGreaterThan(BigDecimal.ZERO);
            assertThat(pkg.get("lastUpdated").asText()).isNotEqualTo(beforeTimestamp);
        }
    }
}
