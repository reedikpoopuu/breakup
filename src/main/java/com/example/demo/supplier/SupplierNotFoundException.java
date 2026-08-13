package com.example.demo.supplier;

public class SupplierNotFoundException extends RuntimeException {

    public SupplierNotFoundException(Long id) {
        super("Supplier not found: " + id);
    }
}
