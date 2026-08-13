package com.example.demo.supplier;

import com.example.demo.common.CountryCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupplierRequest(
        @NotNull CountryCode country,
        @NotBlank String name,
        @NotBlank @Email String rfqEmail,
        @NotBlank String priceUrl
) {
}
