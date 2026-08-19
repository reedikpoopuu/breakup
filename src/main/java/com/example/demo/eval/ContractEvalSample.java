package com.example.demo.eval;

import com.example.demo.common.CountryCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An admin-curated real contract PDF plus the human-verified "correct" extraction for
 * it, used only to check {@code ContractPricingAiExtractor} against real input - see
 * {@code ContractEvalSampleService#runEval}. Deliberately stores the original PDF
 * bytes, unlike a customer's uploaded contract ({@code ContractUploadController} never
 * persists one): these are documents an admin personally obtained and chose to keep
 * specifically for this purpose, not customer data flowing through the normal upload
 * path, so the "never keep the original document" rule doesn't apply here.
 */
@Entity
@Table(name = "contract_eval_samples")
public class ContractEvalSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Lob
    @Column(name = "pdf_bytes", nullable = false)
    private byte[] pdfBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CountryCode country;

    @Column(name = "expected_supplier_name")
    private String expectedSupplierName;

    @Column(name = "expected_plan_name")
    private String expectedPlanName;

    @Column(name = "expected_price_per_kwh", precision = 10, scale = 5)
    private BigDecimal expectedPricePerKwh;

    // scale=5, not 2: a real monthly fee can legitimately have 3 decimal places (e.g.
    // Enefit's 1.656 EUR, verified against a real contract) - scale=2 silently rounded
    // an admin-entered label and produced a false mismatch against the AI's more
    // precise (and correct) answer, caught by exercising this end-to-end for real.
    @Column(name = "expected_monthly_fee_eur", precision = 10, scale = 5)
    private BigDecimal expectedMonthlyFeeEur;

    @Column(name = "expected_contract_type", nullable = false)
    private String expectedContractType;

    @Column(name = "expected_termless", nullable = false)
    private boolean expectedTermless;

    @Column(name = "expected_expiry_date")
    private LocalDate expectedExpiryDate;

    @Column(name = "expected_early_termination_penalty_eur", precision = 10, scale = 2)
    private BigDecimal expectedEarlyTerminationPenaltyEur;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ContractEvalSample() {
        // JPA
    }

    public ContractEvalSample(String fileName, byte[] pdfBytes, CountryCode country,
                               String expectedSupplierName, String expectedPlanName,
                               BigDecimal expectedPricePerKwh, BigDecimal expectedMonthlyFeeEur,
                               String expectedContractType, boolean expectedTermless,
                               LocalDate expectedExpiryDate, BigDecimal expectedEarlyTerminationPenaltyEur) {
        this.fileName = fileName;
        this.pdfBytes = pdfBytes;
        this.country = country;
        this.expectedSupplierName = expectedSupplierName;
        this.expectedPlanName = expectedPlanName;
        this.expectedPricePerKwh = expectedPricePerKwh;
        this.expectedMonthlyFeeEur = expectedMonthlyFeeEur;
        this.expectedContractType = expectedContractType;
        this.expectedTermless = expectedTermless;
        this.expectedExpiryDate = expectedExpiryDate;
        this.expectedEarlyTerminationPenaltyEur = expectedEarlyTerminationPenaltyEur;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getPdfBytes() {
        return pdfBytes;
    }

    public CountryCode getCountry() {
        return country;
    }

    public String getExpectedSupplierName() {
        return expectedSupplierName;
    }

    public String getExpectedPlanName() {
        return expectedPlanName;
    }

    public BigDecimal getExpectedPricePerKwh() {
        return expectedPricePerKwh;
    }

    public BigDecimal getExpectedMonthlyFeeEur() {
        return expectedMonthlyFeeEur;
    }

    public String getExpectedContractType() {
        return expectedContractType;
    }

    public boolean isExpectedTermless() {
        return expectedTermless;
    }

    public LocalDate getExpectedExpiryDate() {
        return expectedExpiryDate;
    }

    public BigDecimal getExpectedEarlyTerminationPenaltyEur() {
        return expectedEarlyTerminationPenaltyEur;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
