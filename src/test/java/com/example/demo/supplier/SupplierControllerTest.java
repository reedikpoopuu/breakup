package com.example.demo.supplier;

import com.example.demo.auth.AppUser;
import com.example.demo.auth.AppUserRepository;
import com.example.demo.auth.Role;
import com.example.demo.auth.TokenService;
import com.example.demo.common.CountryCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupplierControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    TokenService tokenService;

    @Autowired
    ObjectMapper objectMapper;

    String adminAuthHeader() {
        AppUser admin = appUserRepository.findBySmartIdIdentity("EE-38507030022")
                .orElseThrow(() -> new IllegalStateException("test admin not bootstrapped"));
        return "Bearer " + tokenService.issue(admin);
    }

    String userAuthHeader() {
        AppUser user = appUserRepository.save(new AppUser("LV-someone-" + System.nanoTime(), "Some User", Role.USER));
        return "Bearer " + tokenService.issue(user);
    }

    @Nested
    class PublicReadPath {

        @Test
        void returnsSeededActiveSuppliersForCountry() throws Exception {
            mockMvc.perform(get("/api/suppliers").param("country", "EE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)))
                    .andExpect(jsonPath("$[?(@.name == 'Enefit')]").exists());
        }

        @Test
        void returnsSuppliersForEachBalticCountry() throws Exception {
            mockMvc.perform(get("/api/suppliers").param("country", "LV"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
            mockMvc.perform(get("/api/suppliers").param("country", "LT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
        }

        @Test
        void doesNotRequireAuthentication() throws Exception {
            mockMvc.perform(get("/api/suppliers"))
                    .andExpect(status().isOk());
        }

        @Test
        void returnsAllCountriesWhenNoFilterGiven() throws Exception {
            mockMvc.perform(get("/api/suppliers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(11)));
        }
    }

    @Nested
    class AdminReadPath {

        @Test
        void rejectsUnauthenticatedRequests() throws Exception {
            mockMvc.perform(get("/api/admin/suppliers"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void rejectsNonAdminUsers() throws Exception {
            mockMvc.perform(get("/api/admin/suppliers").header("Authorization", userAuthHeader()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void listsAllSuppliersForAdmin() throws Exception {
            mockMvc.perform(get("/api/admin/suppliers").header("Authorization", adminAuthHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(11)));
        }
    }

    @Nested
    class AdminWritePath {

        @Test
        void createsNewSupplier() throws Exception {
            SupplierRequest request = new SupplierRequest(CountryCode.EE, "TestPower-" + System.nanoTime(),
                    "sales@testpower.ee", "https://testpower.ee");

            mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        void rejectsDuplicateCountryAndName() throws Exception {
            SupplierRequest request = new SupplierRequest(CountryCode.EE, "Enefit",
                    "arikliendid@enefit.ee", "https://enefit.ee");

            mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void updatesExistingSupplier() throws Exception {
            SupplierRequest create = new SupplierRequest(CountryCode.LV, "UpdMe-" + System.nanoTime(),
                    "a@b.lv", "https://a.lv");
            String body = mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(create)))
                    .andReturn().getResponse().getContentAsString();
            long id = objectMapper.readTree(body).get("id").asLong();

            SupplierRequest update = new SupplierRequest(CountryCode.LV, "UpdMeToo", "c@d.lv", "https://c.lv");
            mockMvc.perform(put("/api/admin/suppliers/{id}", id)
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("UpdMeToo"));
        }

        @Test
        void softDeletesSupplier() throws Exception {
            SupplierRequest create = new SupplierRequest(CountryCode.LT, "DelMe-" + System.nanoTime(),
                    "a@b.lt", "https://a.lt");
            String body = mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(create)))
                    .andReturn().getResponse().getContentAsString();
            long id = objectMapper.readTree(body).get("id").asLong();

            mockMvc.perform(delete("/api/admin/suppliers/{id}", id).header("Authorization", adminAuthHeader()))
                    .andExpect(status().isNoContent());
        }

        @Test
        void deletedSupplierNoLongerAppearsInPublicList() throws Exception {
            SupplierRequest create = new SupplierRequest(CountryCode.LT, "HideMe-" + System.nanoTime(),
                    "a@b.lt", "https://a.lt");
            String body = mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(create)))
                    .andReturn().getResponse().getContentAsString();
            long id = objectMapper.readTree(body).get("id").asLong();
            String name = objectMapper.readTree(body).get("name").asText();

            mockMvc.perform(delete("/api/admin/suppliers/{id}", id).header("Authorization", adminAuthHeader()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/suppliers").param("country", "LT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.name == '" + name + "')]").isEmpty());
        }

        @Test
        void deletingASupplierAlsoRemovesItsPackagesFromTheAdminPackagesListing() throws Exception {
            String supplierName = "CascadeMe-" + System.nanoTime();
            SupplierRequest create = new SupplierRequest(CountryCode.LT, supplierName, "a@b.lt", "https://a.lt");
            String supplierBody = mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(create)))
                    .andReturn().getResponse().getContentAsString();
            long supplierId = objectMapper.readTree(supplierBody).get("id").asLong();

            String packageBody = mockMvc.perform(post("/api/admin/packages")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"packageName":"Cascade Package","supplierName":"%s","country":"LT","pricePerKwh":0.15,"marginPerKwh":0.02}
                                    """.formatted(supplierName)))
                    .andReturn().getResponse().getContentAsString();
            long packageId = objectMapper.readTree(packageBody).get("id").asLong();

            mockMvc.perform(get("/api/admin/packages").header("Authorization", adminAuthHeader()))
                    .andExpect(jsonPath("$[?(@.id == " + packageId + ")]").exists());

            mockMvc.perform(delete("/api/admin/suppliers/{id}", supplierId).header("Authorization", adminAuthHeader()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/admin/packages").header("Authorization", adminAuthHeader()))
                    .andExpect(jsonPath("$[?(@.id == " + packageId + ")]").doesNotExist());
        }

        @Test
        void nonAdminCannotCreateSuppliers() throws Exception {
            SupplierRequest request = new SupplierRequest(CountryCode.EE, "Blocked", "a@b.ee", "https://a.ee");

            mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", userAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void rejectsBlankSupplierName() throws Exception {
            SupplierRequest request = new SupplierRequest(CountryCode.EE, "", "a@b.ee", "https://a.ee");

            mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsMalformedRfqEmail() throws Exception {
            SupplierRequest request = new SupplierRequest(CountryCode.EE, "BadEmail-" + System.nanoTime(),
                    "not-an-email", "https://a.ee");

            mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsBlankPriceUrl() throws Exception {
            SupplierRequest request = new SupplierRequest(CountryCode.EE, "BadUrl-" + System.nanoTime(),
                    "a@b.ee", "");

            mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsAPriceUrlPointingAtTheCloudMetadataAddress() throws Exception {
            SupplierRequest request = new SupplierRequest(CountryCode.EE, "Ssrf-" + System.nanoTime(),
                    "a@b.ee", "http://169.254.169.254/latest/meta-data/iam/security-credentials/");

            mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsMissingCountry() throws Exception {
            String requestJson = "{\"name\":\"NoCountry\",\"rfqEmail\":\"a@b.ee\",\"priceUrl\":\"https://a.ee\"}";

            mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updatingNonExistentSupplierReturnsNotFound() throws Exception {
            SupplierRequest update = new SupplierRequest(CountryCode.LV, "Ghost", "a@b.lv", "https://a.lv");

            mockMvc.perform(put("/api/admin/suppliers/{id}", 999_999_999L)
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deletingNonExistentSupplierReturnsNotFound() throws Exception {
            mockMvc.perform(delete("/api/admin/suppliers/{id}", 999_999_999L)
                            .header("Authorization", adminAuthHeader()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void renamingSupplierToAnotherActiveSuppliersCountryAndNameIsRejected() throws Exception {
            SupplierRequest create = new SupplierRequest(CountryCode.EE, "RenameMe-" + System.nanoTime(),
                    "a@b.ee", "https://a.ee");
            String body = mockMvc.perform(post("/api/admin/suppliers")
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(create)))
                    .andReturn().getResponse().getContentAsString();
            long id = objectMapper.readTree(body).get("id").asLong();

            SupplierRequest collide = new SupplierRequest(CountryCode.EE, "Enefit", "a@b.ee", "https://a.ee");
            mockMvc.perform(put("/api/admin/suppliers/{id}", id)
                            .header("Authorization", adminAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(collide)))
                    .andExpect(status().isConflict());
        }
    }
}
