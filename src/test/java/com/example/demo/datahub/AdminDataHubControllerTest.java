package com.example.demo.datahub;

import com.example.demo.auth.AppUser;
import com.example.demo.auth.AppUserRepository;
import com.example.demo.auth.Role;
import com.example.demo.auth.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDataHubControllerTest {

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
        AppUser user = appUserRepository.save(new AppUser("LT-someone-" + System.nanoTime(), "Some User", Role.USER));
        return "Bearer " + tokenService.issue(user);
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/admin/datahub/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/datahub/status").header("Authorization", userAuthHeader()))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportsAllThreeAdaptersAsUnconfiguredWhenNoCredentialsAreSet() throws Exception {
        mockMvc.perform(get("/api/admin/datahub/status").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.source == 'ESTFEED' && @.country == 'EE')]").exists())
                .andExpect(jsonPath("$[?(@.source == 'STEP' && @.country == 'LV')]").exists())
                .andExpect(jsonPath("$[?(@.source == 'ESO' && @.country == 'LT')]").exists())
                .andExpect(jsonPath("$[?(@.configured == true)]").doesNotExist())
                .andExpect(jsonPath("$[?(@.credentialsSet == true)]").doesNotExist());
    }
}
