package com.example.demo.supplier;

import com.example.demo.common.CountryCode;

import java.time.Instant;

public record SupplierResponse(
        Long id,
        CountryCode country,
        String name,
        String rfqEmail,
        String priceUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getCountry(),
                supplier.getName(),
                supplier.getRfqEmail(),
                supplier.getPriceUrl(),
                supplier.isActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }
}
