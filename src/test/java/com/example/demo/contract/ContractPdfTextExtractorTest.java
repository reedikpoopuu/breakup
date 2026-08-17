package com.example.demo.contract;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractPdfTextExtractorTest {

    private final ContractPdfTextExtractor extractor = new ContractPdfTextExtractor();

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
    void extractsTextFromARealPdf() throws IOException {
        byte[] pdf = pdfContaining("EIC 38ZEE-1000009--Z Registrikood 12345678");

        String text = extractor.extractText(pdf);

        assertThat(text).contains("38ZEE-1000009--Z");
        assertThat(text).contains("12345678");
    }

    @Test
    void throwsAClearExceptionForNonPdfBytes() {
        byte[] notAPdf = "this is not a pdf".getBytes();

        assertThatThrownBy(() -> extractor.extractText(notAPdf))
                .isInstanceOf(ContractPdfUnreadableException.class);
    }
}
