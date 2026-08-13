package com.example.demo.supplier;

import com.example.demo.common.CountryCode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the real, PM-supplied supplier list on first boot. Suppliers remain
 * administrator-editable afterward via {@link AdminSupplierController} - this only
 * establishes the starting rows, matching ARCH_SPEC.md section 1.2.
 */
@Component
public class SupplierSeeder implements CommandLineRunner {

    private record SeedRow(CountryCode country, String name, String rfqEmail, String priceUrl) {
    }

    private static final List<SeedRow> SEED_DATA = List.of(
            new SeedRow(CountryCode.EE, "Enefit", "arikliendid@enefit.ee",
                    "https://www.enefit.ee/et/era/elekter/elektrileping-ja-paketid#/"),
            new SeedRow(CountryCode.EE, "Alexela", "ariklient@alexela.ee",
                    "https://www.alexela.ee/et/elekter"),
            new SeedRow(CountryCode.EE, "Elenger", "klienditugi@elenger.ee",
                    "https://elenger.ee/kodukliendile/elekter/"),
            new SeedRow(CountryCode.EE, "Elektrum", "myyk@elektrum.ee",
                    "https://www.elektrum.ee/ee/eraklient/elekter/elektripaketid"),
            new SeedRow(CountryCode.EE, "Sunly", "elekter@sunly.ee",
                    "https://sunly.ee/elekter/ari"),

            new SeedRow(CountryCode.LV, "Elektrum", "klientu.serviss@elektrum.lv",
                    "https://www.elektrum.lv/lv/majai/klientiem/elektribas-podukta-izvele/produkti/#produkti"),
            new SeedRow(CountryCode.LV, "Enefit", "bizness@enefit.lv",
                    "https://www.enefit.lv/lv/majai/elektriba#/"),
            new SeedRow(CountryCode.LV, "Virši", "info@virsi.lv",
                    "https://www.virsi.lv/lv/privatpersonam/elektriba/elektriba"),

            new SeedRow(CountryCode.LT, "Ignitis", "info@ignitis.lt",
                    "https://ignitis.lt/elektros-kainu-skaiciuokle/elektros-planai"),
            new SeedRow(CountryCode.LT, "Enefit", "energija@enefit.lt",
                    "https://www.enefit.lt/lt/privatiems/elektra#/"),
            new SeedRow(CountryCode.LT, "Elektrum", "info@elektrum.lt",
                    "https://www.elektrum.lt/lt/namams/elektra")
    );

    private final SupplierRepository repository;

    public SupplierSeeder(SupplierRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        for (SeedRow row : SEED_DATA) {
            if (!repository.existsByCountryAndName(row.country(), row.name())) {
                repository.save(new Supplier(row.country(), row.name(), row.rfqEmail(), row.priceUrl()));
            }
        }
    }
}
