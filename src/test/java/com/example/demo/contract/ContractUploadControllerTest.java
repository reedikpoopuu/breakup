package com.example.demo.contract;

import com.example.demo.auth.AppUser;
import com.example.demo.auth.AppUserRepository;
import com.example.demo.auth.Role;
import com.example.demo.auth.TokenService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContractUploadControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    TokenService tokenService;

    private String userAuthHeader() {
        AppUser user = appUserRepository.save(new AppUser("LV-contract-" + System.nanoTime(), "Some User", Role.USER));
        return "Bearer " + tokenService.issue(user);
    }

    private static byte[] pdfContaining(String line) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(line);
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf", pdfContaining("EIC 38ZEE-1000009--Z"));

        mockMvc.perform(multipart("/api/contracts/parse").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anyAuthenticatedUserCanParseTheirOwnContractNotJustAdmins() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf",
                pdfContaining("EIC 38ZEE-1000009--Z Registrikood 10421629"));

        mockMvc.perform(multipart("/api/contracts/parse").file(file).header("Authorization", userAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eicCodes[0]").value("38ZEE-1000009--Z"))
                .andExpect(jsonPath("$.registryCodeCandidates[0].country").value("EE"))
                .andExpect(jsonPath("$.registryCodeCandidates[0].code").value("10421629"));
    }

    @Test
    void rejectsNonPdfUploadsWithBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "contract.txt", "text/plain", "not a pdf".getBytes());

        mockMvc.perform(multipart("/api/contracts/parse").file(file).header("Authorization", userAuthHeader()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsEmptyUploadsWithBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/contracts/parse").file(file).header("Authorization", userAuthHeader()))
                .andExpect(status().isBadRequest());
    }
}
