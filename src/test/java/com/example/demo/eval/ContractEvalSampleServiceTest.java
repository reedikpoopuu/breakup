package com.example.demo.eval;

import com.example.demo.common.CountryCode;
import com.example.demo.contract.AiExtractedPricingFields;
import com.example.demo.contract.ContractPdfTextExtractor;
import com.example.demo.contract.ContractPdfUnreadableException;
import com.example.demo.contract.ContractPricingAiExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractEvalSampleServiceTest {

    private final ContractEvalSampleRepository repository = mock(ContractEvalSampleRepository.class);
    private final ContractPdfTextExtractor textExtractor = mock(ContractPdfTextExtractor.class);
    private final ContractPricingAiExtractor pricingAiExtractor = mock(ContractPricingAiExtractor.class);
    private final ContractEvalSampleService service =
            new ContractEvalSampleService(repository, textExtractor, pricingAiExtractor);

    private static MockMultipartFile pdf(String content) {
        return new MockMultipartFile("file", "sample.pdf", "application/pdf", content.getBytes());
    }

    // ---- save ----

    @Test
    void savePersistsAValidSample() {
        when(textExtractor.extractText(any())).thenReturn("some contract text");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractEvalSampleResponse result = service.save(pdf("%PDF-1.4 fake"), CountryCode.EE, "Enefit AS", "Muutuv",
                new BigDecimal("0.00287"), new BigDecimal("1.656"), "SPOT", true, null, null);

        assertThat(result.fileName()).isEqualTo("sample.pdf");
        assertThat(result.country()).isEqualTo(CountryCode.EE);
        assertThat(result.expectedContractType()).isEqualTo("SPOT");
        assertThat(result.expectedTermless()).isTrue();
        verify(repository).save(any());
    }

    @Test
    void saveRejectsAContractTypeOutsideFixedOrSpot() {
        assertThatThrownBy(() -> service.save(pdf("x"), CountryCode.EE, null, null, null, null,
                "VARIABLE", false, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveRejectsAnEmptyFile() {
        assertThatThrownBy(() -> service.save(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]),
                CountryCode.EE, null, null, null, null, "FIXED", false, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void savePropagatesAnUnreadablePdfRatherThanStoringIt() {
        when(textExtractor.extractText(any())).thenThrow(new ContractPdfUnreadableException("bad pdf"));

        assertThatThrownBy(() -> service.save(pdf("garbage"), CountryCode.EE, null, null, null, null,
                "FIXED", false, null, null))
                .isInstanceOf(ContractPdfUnreadableException.class);
    }

    // ---- runEval ----

    private static ContractEvalSample sample(String contractType, boolean termless) {
        return new ContractEvalSample("enefit.pdf", "text".getBytes(), CountryCode.EE, "Enefit AS", "Muutuv",
                new BigDecimal("0.00287"), new BigDecimal("1.656"), contractType, termless, null, null);
    }

    @Test
    void runEvalReportsAiUnavailableRatherThanFailingWhenNoProviderIsConfigured() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sample("SPOT", true)));
        when(textExtractor.extractText(any())).thenReturn("text");
        when(pricingAiExtractor.extract(any())).thenReturn(Optional.empty());

        List<ContractEvalSampleResult> results = service.runEval();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).aiAvailable()).isFalse();
        assertThat(results.get(0).fields()).isEmpty();
    }

    @Test
    void runEvalFlagsAStoredPdfThatCanNoLongerBeRead() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sample("SPOT", true)));
        when(textExtractor.extractText(any())).thenThrow(new ContractPdfUnreadableException("corrupted"));

        List<ContractEvalSampleResult> results = service.runEval();

        assertThat(results.get(0).error()).contains("corrupted");
        assertThat(results.get(0).fields()).isEmpty();
    }

    @Test
    void runEvalMatchesEveryFieldWhenTheExtractionIsExactlyRight() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sample("SPOT", true)));
        when(textExtractor.extractText(any())).thenReturn("text");
        when(pricingAiExtractor.extract(any())).thenReturn(Optional.of(new AiExtractedPricingFields(
                "Enefit AS", "Muutuv", new BigDecimal("0.00287"), new BigDecimal("1.656"),
                "SPOT", true, null, null, "")));

        List<ContractEvalFieldResult> fields = service.runEval().get(0).fields();

        assertThat(fields).allMatch(ContractEvalFieldResult::match);
    }

    @Test
    void runEvalFlagsANumericMismatchEvenWhenScaleDiffers() {
        // 0.00287 vs 0.0028700 - same value, different BigDecimal scale, must still match.
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sample("SPOT", true)));
        when(textExtractor.extractText(any())).thenReturn("text");
        when(pricingAiExtractor.extract(any())).thenReturn(Optional.of(new AiExtractedPricingFields(
                "Enefit AS", "Muutuv", new BigDecimal("0.0028700"), new BigDecimal("9.99"),
                "SPOT", true, null, null, "")));

        List<ContractEvalFieldResult> fields = service.runEval().get(0).fields();

        ContractEvalFieldResult price = fields.stream().filter(f -> f.field().equals("pricePerKwh")).findFirst().orElseThrow();
        assertThat(price.match()).as("same numeric value, different scale, must still match").isTrue();

        ContractEvalFieldResult fee = fields.stream().filter(f -> f.field().equals("monthlyFeeEur")).findFirst().orElseThrow();
        assertThat(fee.match()).as("expected 1.656, got 9.99 - genuine mismatch").isFalse();
    }

    @Test
    void runEvalTreatsSupplierNameCaseAndWhitespaceDifferencesAsAMatch() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sample("SPOT", true)));
        when(textExtractor.extractText(any())).thenReturn("text");
        when(pricingAiExtractor.extract(any())).thenReturn(Optional.of(new AiExtractedPricingFields(
                "  enefit as  ", "Muutuv", new BigDecimal("0.00287"), new BigDecimal("1.656"),
                "SPOT", true, null, null, "")));

        ContractEvalFieldResult supplier = service.runEval().get(0).fields().stream()
                .filter(f -> f.field().equals("supplierName")).findFirst().orElseThrow();

        assertThat(supplier.match()).isTrue();
    }

    @Test
    void runEvalFlagsAContractTypeMismatch() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sample("FIXED", false)));
        when(textExtractor.extractText(any())).thenReturn("text");
        when(pricingAiExtractor.extract(any())).thenReturn(Optional.of(new AiExtractedPricingFields(
                "Enefit AS", "Muutuv", new BigDecimal("0.15"), null,
                "SPOT", false, null, null, "")));

        ContractEvalFieldResult type = service.runEval().get(0).fields().stream()
                .filter(f -> f.field().equals("contractType")).findFirst().orElseThrow();

        assertThat(type.match()).isFalse();
        assertThat(type.expected()).isEqualTo("FIXED");
        assertThat(type.actual()).isEqualTo("SPOT");
    }

    // ---- list / delete ----

    @Test
    void listMapsSamplesWithoutExposingPdfBytes() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sample("SPOT", true)));

        List<ContractEvalSampleResponse> result = service.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fileName()).isEqualTo("enefit.pdf");
    }

    @Test
    void deleteDelegatesToTheRepository() {
        service.delete(42L);
        verify(repository).deleteById(42L);
    }
}
