package com.example.demo.eval;

import com.example.demo.common.CountryCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** {@link ContractEvalSample} without the PDF bytes, for listing - the admin already has the file. */
public record ContractEvalSampleResponse(
        Long id,
        String fileName,
        CountryCode country,
        String expectedSupplierName,
        String expectedPlanName,
        BigDecimal expectedPricePerKwh,
        BigDecimal expectedMonthlyFeeEur,
        String expectedContractType,
        boolean expectedTermless,
        LocalDate expectedExpiryDate,
        BigDecimal expectedEarlyTerminationPenaltyEur,
        Instant createdAt
) {
    public static ContractEvalSampleResponse from(ContractEvalSample sample) {
        return new ContractEvalSampleResponse(
                sample.getId(), sample.getFileName(), sample.getCountry(),
                sample.getExpectedSupplierName(), sample.getExpectedPlanName(),
                sample.getExpectedPricePerKwh(), sample.getExpectedMonthlyFeeEur(),
                sample.getExpectedContractType(), sample.isExpectedTermless(),
                sample.getExpectedExpiryDate(), sample.getExpectedEarlyTerminationPenaltyEur(),
                sample.getCreatedAt());
    }
}
