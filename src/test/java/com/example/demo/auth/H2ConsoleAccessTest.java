package com.example.demo.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H2's web console is a full read/write SQL console over the whole database - it must
 * never be reachable without a real ROLE_ADMIN session, independent of whether {@code
 * spring.h2.console.enabled} happens to be on (see application.properties / SecurityConfig).
 * Enabling it explicitly here via TestPropertySource so this test proves the ROLE_ADMIN
 * gate itself, not just that the console is off.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.h2.console.enabled=true")
class H2ConsoleAccessTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    TokenService tokenService;

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsNonAdminUsers() throws Exception {
        AppUser user = appUserRepository.save(new AppUser("LV-h2console-" + System.nanoTime(), "Some User", Role.USER));
        String token = "Bearer " + tokenService.issue(user);

        mockMvc.perform(get("/h2-console/").header("Authorization", token))
                .andExpect(status().isForbidden());
    }
}
