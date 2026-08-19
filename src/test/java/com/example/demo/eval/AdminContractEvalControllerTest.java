package com.example.demo.eval;

import com.example.demo.auth.AppUser;
import com.example.demo.auth.AppUserRepository;
import com.example.demo.auth.Role;
import com.example.demo.auth.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminContractEvalControllerTest {

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
        mockMvc.perform(get("/api/admin/eval-samples"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/eval-samples").header("Authorization", userAuthHeader()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsNoSamplesInitially() throws Exception {
        mockMvc.perform(get("/api/admin/eval-samples").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAnUploadWithAnInvalidContractType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "%PDF-1.4".getBytes());
        mockMvc.perform(multipart("/api/admin/eval-samples")
                        .file(file)
                        .param("country", "EE")
                        .param("contractType", "NOT_A_TYPE")
                        .header("Authorization", adminAuthHeader()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnUploadWithAnUnreadablePdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "not a real pdf".getBytes());
        mockMvc.perform(multipart("/api/admin/eval-samples")
                        .file(file)
                        .param("country", "EE")
                        .param("contractType", "FIXED")
                        .header("Authorization", adminAuthHeader()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingAnUnknownSampleIsRejectedForNonAdmins() throws Exception {
        mockMvc.perform(delete("/api/admin/eval-samples/1").header("Authorization", userAuthHeader()))
                .andExpect(status().isForbidden());
    }
}
