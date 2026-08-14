package com.example.demo.pricing;

import com.example.demo.common.CountryCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Read model for default energy offers plus a simulated price-scraper trigger.
 * There is no live scraping infrastructure yet (ARCH_SPEC.md has no such adapter) -
 * {@link #runScraper()} re-derives prices with a small randomized market drift so the
 * Admin Control Room's "Run Price Scraper Now" action is honest about being a
 * placeholder while still exercising the real persistence path.
 */
@Service
public class EnergyPackageService {

    private static final BigDecimal MAX_DRIFT = new BigDecimal("0.04");
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.05");

    private final EnergyPackageRepository repository;

    public EnergyPackageService(EnergyPackageRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EnergyPackage> listAll(CountryCode country) {
        return country == null ? repository.findAll() : repository.findByCountry(country);
    }

    @Transactional
    public List<EnergyPackage> runScraper() {
        List<EnergyPackage> packages = repository.findAll();
        for (EnergyPackage energyPackage : packages) {
            BigDecimal price = drift(energyPackage.getPricePerKwh());
            BigDecimal margin = drift(energyPackage.getMarginPerKwh());
            energyPackage.applyScrapedPrice(price, margin);
        }
        return packages;
    }

    private static BigDecimal drift(BigDecimal value) {
        double factor = 1 + ThreadLocalRandom.current().nextDouble(-1, 1) * MAX_DRIFT.doubleValue();
        BigDecimal drifted = value.multiply(BigDecimal.valueOf(factor)).setScale(4, RoundingMode.HALF_UP);
        return drifted.max(MIN_PRICE);
    }
}
