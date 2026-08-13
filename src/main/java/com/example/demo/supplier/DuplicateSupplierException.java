package com.example.demo.supplier;

import com.example.demo.common.CountryCode;

public class DuplicateSupplierException extends RuntimeException {

    public DuplicateSupplierException(CountryCode country, String name) {
        super("Supplier already exists: " + country + "/" + name);
    }
}
