package com.example.demo.eval;

import com.example.demo.common.CountryCode;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Admin-only (see SecurityConfig's {@code /api/admin/**} rule). Lets an admin build up
 * a small library of real, hand-labeled contract PDFs and check
 * {@code ContractPricingAiExtractor}'s real output against them on demand - see
 * {@link ContractEvalSampleService}.
 */
@RestController
@RequestMapping("/api/admin/eval-samples")
public class AdminContractEvalController {

    private final ContractEvalSampleService service;

    public AdminContractEvalController(ContractEvalSampleService service) {
        this.service = service;
    }

    @GetMapping
    public List<ContractEvalSampleResponse> list() {
        return service.list();
    }

    @PostMapping
    public ContractEvalSampleResponse upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam CountryCode country,
                                              @RequestParam(required = false) String supplierName,
                                              @RequestParam(required = false) String planName,
                                              @RequestParam(required = false) BigDecimal pricePerKwh,
                                              @RequestParam(required = false) BigDecimal monthlyFeeEur,
                                              @RequestParam String contractType,
                                              @RequestParam(defaultValue = "false") boolean termless,
                                              @RequestParam(required = false) LocalDate expiryDate,
                                              @RequestParam(required = false) BigDecimal earlyTerminationPenaltyEur) {
        return service.save(file, country, supplierName, planName, pricePerKwh, monthlyFeeEur,
                contractType, termless, expiryDate, earlyTerminationPenaltyEur);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /**
     * Calls the real, configured AI provider once per stored sample - a deliberate,
     * admin-triggered action (matches {@code POST /api/admin/packages/scrape}'s
     * on-demand pattern), never automatic, since this is billed API usage.
     */
    @PostMapping("/run")
    public List<ContractEvalSampleResult> run() {
        return service.runEval();
    }
}
