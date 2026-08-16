package com.example.demo.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * app.admin.smartid-identities is operational/secret data - it must never be
 * hard-coded, and until it is supplied the app should still boot but admin login
 * fails closed (ARCH_SPEC.md section 2.2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.admin.smartid-identities=")
class AdminBootstrapFailClosedTest {

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    MockMvc mockMvc;

    @Test
    void noAdminRowIsSeededWhenIdentityIsUnset() {
        assertThat(appUserRepository.existsByRole(Role.ADMIN)).isFalse();
    }

    @Test
    void adminEndpointsAreUnreachableSinceNoAdminExists() throws Exception {
        mockMvc.perform(get("/api/admin/suppliers"))
                .andExpect(status().isUnauthorized());
    }
}
