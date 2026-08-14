package com.example.demo.pricing;

import com.example.demo.common.CountryCode;

import java.math.BigDecimal;
import java.time.Instant;

public record EnergyPackageResponse(
        Long id,
        String packageName,
        String supplierName,
        CountryCode country,
        BigDecimal pricePerKwh,
        BigDecimal marginPerKwh,
        Instant lastUpdated
) {
    public static EnergyPackageResponse from(EnergyPackage energyPackage) {
        return new EnergyPackageResponse(
                energyPackage.getId(),
                energyPackage.getPackageName(),
                energyPackage.getSupplierName(),
                energyPackage.getCountry(),
                energyPackage.getPricePerKwh(),
                energyPackage.getMarginPerKwh(),
                energyPackage.getLastUpdated()
        );
    }
}
