package com.example.demo.eval;

import com.example.demo.common.CountryCode;
import com.example.demo.contract.AiExtractedPricingFields;
import com.example.demo.contract.ContractPdfTextExtractor;
import com.example.demo.contract.ContractPricingAiExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Runs {@link ContractPricingAiExtractor} against real, admin-labeled contract PDFs and
 * reports how it did - the thing {@code ContractPricingAiExtractorTest} structurally
 * cannot do, since every one of those tests mocks the AI response. This is the only
 * place in the app that exercises the real prompt against real input as a repeatable
 * check rather than a one-off manual verification.
 */
@Service
public class ContractEvalSampleService {

    private static final Set<String> ALLOWED_CONTRACT_TYPES = Set.of("FIXED", "SPOT");

    private final ContractEvalSampleRepository repository;
    private final ContractPdfTextExtractor textExtractor;
    private final ContractPricingAiExtractor pricingAiExtractor;

    public ContractEvalSampleService(ContractEvalSampleRepository repository, ContractPdfTextExtractor textExtractor,
                                      ContractPricingAiExtractor pricingAiExtractor) {
        this.repository = repository;
        this.textExtractor = textExtractor;
        this.pricingAiExtractor = pricingAiExtractor;
    }

    public ContractEvalSampleResponse save(MultipartFile file, CountryCode country, String supplierName,
                                            String planName, BigDecimal pricePerKwh, BigDecimal monthlyFeeEur,
                                            String contractType, boolean termless, LocalDate expiryDate,
                                            BigDecimal earlyTerminationPenaltyEur) {
        if (!ALLOWED_CONTRACT_TYPES.contains(contractType)) {
            throw new IllegalArgumentException("contractType must be FIXED or SPOT");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        byte[] pdfBytes;
        try {
            pdfBytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file", e);
        }
        // Fail fast on an unreadable PDF at upload time (throws ContractPdfUnreadableException)
        // rather than only discovering it later when the eval is actually run.
        textExtractor.extractText(pdfBytes);

        ContractEvalSample sample = new ContractEvalSample(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "contract.pdf",
                pdfBytes, country, supplierName, planName, pricePerKwh, monthlyFeeEur,
                contractType, termless, expiryDate, earlyTerminationPenaltyEur);
        return ContractEvalSampleResponse.from(repository.save(sample));
    }

    public List<ContractEvalSampleResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(ContractEvalSampleResponse::from).toList();
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<ContractEvalSampleResult> runEval() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::runOne).toList();
    }

    private ContractEvalSampleResult runOne(ContractEvalSample sample) {
        String text;
        try {
            text = textExtractor.extractText(sample.getPdfBytes());
        } catch (RuntimeException e) {
            return new ContractEvalSampleResult(sample.getId(), sample.getFileName(), true, List.of(),
                    "Could not read stored PDF: " + e.getMessage());
        }

        Optional<AiExtractedPricingFields> extracted = pricingAiExtractor.extract(text);
        if (extracted.isEmpty()) {
            return new ContractEvalSampleResult(sample.getId(), sample.getFileName(), false, List.of(), null);
        }

        AiExtractedPricingFields actual = extracted.get();
        List<ContractEvalFieldResult> fields = new ArrayList<>();
        fields.add(textField("supplierName", sample.getExpectedSupplierName(), actual.supplierName()));
        fields.add(textField("planName", sample.getExpectedPlanName(), actual.planName()));
        fields.add(decimalField("pricePerKwh", sample.getExpectedPricePerKwh(), actual.pricePerKwh()));
        fields.add(decimalField("monthlyFeeEur", sample.getExpectedMonthlyFeeEur(), actual.monthlyFeeEur()));
        fields.add(exactField("contractType", sample.getExpectedContractType(), actual.contractType()));
        fields.add(exactField("termless", String.valueOf(sample.isExpectedTermless()),
                String.valueOf(Boolean.TRUE.equals(actual.termless()))));
        fields.add(exactField("expiryDate", asString(sample.getExpectedExpiryDate()), actual.expiryDate()));
        fields.add(decimalField("earlyTerminationPenaltyEur", sample.getExpectedEarlyTerminationPenaltyEur(),
                actual.earlyTerminationPenaltyEur()));
        return new ContractEvalSampleResult(sample.getId(), sample.getFileName(), true, fields, null);
    }

    private static ContractEvalFieldResult exactField(String name, String expected, String actual) {
        boolean match = normalize(expected).equals(normalize(actual));
        return new ContractEvalFieldResult(name, expected, actual, match);
    }

    /** Case/whitespace-insensitive hint only - see ContractEvalFieldResult's javadoc. */
    private static ContractEvalFieldResult textField(String name, String expected, String actual) {
        boolean match = normalize(expected).equalsIgnoreCase(normalize(actual));
        return new ContractEvalFieldResult(name, expected, actual, match);
    }

    private static ContractEvalFieldResult decimalField(String name, BigDecimal expected, BigDecimal actual) {
        boolean match = expected == null ? actual == null
                : actual != null && expected.compareTo(actual) == 0;
        return new ContractEvalFieldResult(name, asString(expected), asString(actual), match);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
