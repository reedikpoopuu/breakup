package com.example.demo.contract;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Any authenticated user can upload their own contract PDF (not admin-only - this is a
 * customer self-service step). Nothing is persisted here: extraction is synchronous
 * and the result is handed back for the caller to confirm before anything is saved.
 */
@RestController
@RequestMapping("/api/contracts")
public class ContractUploadController {

    private final ContractPdfTextExtractor textExtractor;
    private final ContractFieldExtractor fieldExtractor;
    private final ContractPricingAiExtractor pricingAiExtractor;

    public ContractUploadController(ContractPdfTextExtractor textExtractor, ContractFieldExtractor fieldExtractor,
                                     ContractPricingAiExtractor pricingAiExtractor) {
        this.textExtractor = textExtractor;
        this.fieldExtractor = fieldExtractor;
        this.pricingAiExtractor = pricingAiExtractor;
    }

    @PostMapping("/parse")
    public ExtractedContractFields parse(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ContractPdfUnreadableException(e);
        }
        String text = textExtractor.extractText(bytes);
        ExtractedContractFields regexFields = fieldExtractor.extract(text);
        AiExtractedPricingFields pricingFields = pricingAiExtractor.extract(text).orElse(null);
        return new ExtractedContractFields(regexFields.eicCodes(), regexFields.registryCodeCandidates(), pricingFields);
    }
}
