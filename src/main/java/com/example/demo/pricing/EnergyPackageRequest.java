package com.example.demo.pricing;

import com.example.demo.common.CountryCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EnergyPackageRequest(
        @NotBlank String packageName,
        @NotBlank String supplierName,
        @NotNull CountryCode country,
        @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal pricePerKwh,
        @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal marginPerKwh
) {
}
