package com.example.demo.auth;

import com.example.demo.auth.support.SmartIdTestConfig;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SmartIdTestConfig.class)
class SmartIdAuthenticationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    ObjectMapper objectMapper;

    private String startSession(String identity) throws Exception {
        String body = mockMvc.perform(post("/auth/smartid/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"smartIdIdentity\":\"" + identity + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationCode").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("sessionId").asText();
    }

    @Test
    void startReturnsSessionIdAndVerificationCode() throws Exception {
        mockMvc.perform(post("/auth/smartid/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"smartIdIdentity\":\"LV-12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.verificationCode").value("1234"));
    }

    @Test
    void firstPollReturnsRunning() throws Exception {
        String sessionId = startSession("LV-11111");

        mockMvc.perform(get("/auth/smartid/poll").param("sessionId", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void secondPollIssuesTokenAndCreatesNewUser() throws Exception {
        String identity = "LT-22222-" + System.nanoTime();
        String sessionId = startSession(identity);
        mockMvc.perform(get("/auth/smartid/poll").param("sessionId", sessionId));

        String body = mockMvc.perform(get("/auth/smartid/poll").param("sessionId", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETE"))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(appUserRepository.findBySmartIdIdentity(identity)).isPresent();
        assertThat(json.get("role").asText()).isEqualTo("USER");
    }

    @Test
    void repeatLoginResolvesSameUserRowInsteadOfCreatingAnother() throws Exception {
        String identity = "EE-33333-" + System.nanoTime();

        String firstSession = startSession(identity);
        mockMvc.perform(get("/auth/smartid/poll").param("sessionId", firstSession));
        mockMvc.perform(get("/auth/smartid/poll").param("sessionId", firstSession))
                .andExpect(status().isOk());
        long firstCount = appUserRepository.count();

        String secondSession = startSession(identity);
        mockMvc.perform(get("/auth/smartid/poll").param("sessionId", secondSession));
        mockMvc.perform(get("/auth/smartid/poll").param("sessionId", secondSession))
                .andExpect(status().isOk());

        assertThat(appUserRepository.count()).isEqualTo(firstCount);
    }

    @Test
    void adminIdentityLoginGrantsAccessToAdminEndpoint() throws Exception {
        String sessionId = startSession("EE-38507030022");
        mockMvc.perform(get("/auth/smartid/poll").param("sessionId", sessionId));
        String body = mockMvc.perform(get("/auth/smartid/poll").param("sessionId", sessionId))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asText();

        mockMvc.perform(get("/api/admin/suppliers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
