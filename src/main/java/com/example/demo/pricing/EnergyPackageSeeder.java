package com.example.demo.pricing;

import com.example.demo.common.CountryCode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds default energy offers so the Admin Control Room's package matrix isn't empty on
 * first boot. Every {@code supplierName} here must match a real supplier {@code name}
 * seeded by {@code SupplierSeeder} - a name that doesn't (e.g. a past "Eesti Energia"
 * entry, which isn't a real switchable supplier brand) reappears on every restart with
 * no way to remove it permanently, since this runs unconditionally at each boot.
 */
@Component
public class EnergyPackageSeeder implements CommandLineRunner {

    private record SeedRow(String packageName, String supplierName, CountryCode country,
                            BigDecimal pricePerKwh, BigDecimal marginPerKwh) {
    }

    private static final List<SeedRow> SEED_DATA = List.of(
            new SeedRow("Enefit Kindel 12", "Enefit", CountryCode.EE,
                    new BigDecimal("0.1420"), new BigDecimal("0.0180")),
            new SeedRow("Alexela Kindel", "Alexela", CountryCode.EE,
                    new BigDecimal("0.1390"), new BigDecimal("0.0165")),
            new SeedRow("Elektrum Fix 12", "Elektrum", CountryCode.EE,
                    new BigDecimal("0.1450"), new BigDecimal("0.0190")),

            new SeedRow("Elektrum Fikss", "Elektrum", CountryCode.LV,
                    new BigDecimal("0.1510"), new BigDecimal("0.0200")),
            new SeedRow("Enefit Fiksēts", "Enefit", CountryCode.LV,
                    new BigDecimal("0.1470"), new BigDecimal("0.0175")),

            new SeedRow("Ignitis Standartas", "Ignitis", CountryCode.LT,
                    new BigDecimal("0.1530"), new BigDecimal("0.0210")),
            new SeedRow("Enefit Fiksuotas", "Enefit", CountryCode.LT,
                    new BigDecimal("0.1495"), new BigDecimal("0.0185")),
            new SeedRow("Elektrum Vienoda", "Elektrum", CountryCode.LT,
                    new BigDecimal("0.1505"), new BigDecimal("0.0195"))
    );

    private final EnergyPackageRepository repository;

    public EnergyPackageSeeder(EnergyPackageRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        for (SeedRow row : SEED_DATA) {
            if (!repository.existsBySupplierNameAndPackageName(row.supplierName(), row.packageName())) {
                repository.save(new EnergyPackage(row.packageName(), row.supplierName(), row.country(),
                        row.pricePerKwh(), row.marginPerKwh(), PackageSource.MANUAL));
            }
        }
    }
}
