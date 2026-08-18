package com.example.demo.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogoutControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    TokenService tokenService;

    private String issueTokenFor(String identity) {
        AppUser user = appUserRepository.save(new AppUser(identity, "Some User", Role.USER));
        return tokenService.issue(user);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logsOutAndTheSameTokenIsRejectedAfterwards() throws Exception {
        String token = issueTokenFor("LV-logout-" + System.nanoTime());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // /api/admin/datahub/status just needs any authenticated (here: revoked, so
        // rejected) principal to prove the token no longer passes BearerTokenAuthFilter -
        // it doesn't matter that this particular user isn't ADMIN, 401 comes first.
        mockMvc.perform(get("/api/admin/datahub/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherSessionsForTheSameUserAreUnaffectedByOneLogout() throws Exception {
        String identity = "LV-logout-" + System.nanoTime();
        AppUser user = appUserRepository.save(new AppUser(identity, "Some User", Role.USER));
        String firstToken = tokenService.issue(user);
        String secondToken = tokenService.issue(user);

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/datahub/status").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isForbidden());
    }
}
