package com.example.demo.pricing;

public class EnergyPackageNotFoundException extends RuntimeException {

    public EnergyPackageNotFoundException(Long id) {
        super("Energy package not found: " + id);
    }
}
