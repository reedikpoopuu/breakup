package com.example.demo.pricing;

import com.example.demo.common.CountryCode;
import com.example.demo.pricing.scrape.ScrapedOffer;
import com.example.demo.pricing.scrape.SupplierPageFetchException;
import com.example.demo.pricing.scrape.SupplierPageFetcher;
import com.example.demo.pricing.scrape.SupplierPriceParser;
import com.example.demo.supplier.Supplier;
import com.example.demo.supplier.SupplierRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Read model for default energy offers, plus the real price scraper behind the Admin
 * Control Room's "Run Price Scraper Now" action. Only suppliers with a registered
 * {@link SupplierPriceParser} are actually scraped (their page structure was verified
 * by hand - see each parser's javadoc); every other supplier's packages are
 * {@link PackageSource#MANUAL} and admin-managed via {@link #create}/{@link #delete}.
 * A parser is matched to its live {@code priceUrl} via the {@code Supplier} row
 * ({@code SupplierRepository}) with the same country + name; a missing/inactive
 * supplier or a fetch failure only skips that one supplier; it never aborts the run.
 */
@Service
public class EnergyPackageService {

    private static final Logger log = LoggerFactory.getLogger(EnergyPackageService.class);

    private final EnergyPackageRepository repository;
    private final SupplierRepository supplierRepository;
    private final List<SupplierPriceParser> parsers;
    private final SupplierPageFetcher fetcher;

    public EnergyPackageService(EnergyPackageRepository repository, SupplierRepository supplierRepository,
                                 List<SupplierPriceParser> parsers, SupplierPageFetcher fetcher) {
        this.repository = repository;
        this.supplierRepository = supplierRepository;
        this.parsers = parsers;
        this.fetcher = fetcher;
    }

    @Transactional(readOnly = true)
    public List<EnergyPackage> listAll(CountryCode country) {
        return country == null ? repository.findAll() : repository.findByCountry(country);
    }

    @Transactional(readOnly = true)
    public List<EnergyPackage> listVisible(CountryCode country) {
        return country == null ? repository.findByVisibleTrue() : repository.findByCountryAndVisibleTrue(country);
    }

    @Transactional
    public EnergyPackage create(EnergyPackageRequest request) {
        EnergyPackage energyPackage = new EnergyPackage(request.packageName(), request.supplierName(),
                request.country(), request.pricePerKwh(), request.marginPerKwh(), PackageSource.MANUAL);
        return repository.save(energyPackage);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EnergyPackageNotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public EnergyPackage setVisible(Long id, boolean visible) {
        EnergyPackage energyPackage = repository.findById(id).orElseThrow(() -> new EnergyPackageNotFoundException(id));
        energyPackage.setVisible(visible);
        return energyPackage;
    }

    @Transactional
    public List<EnergyPackage> runScraper() {
        for (SupplierPriceParser parser : parsers) {
            scrapeOneSupplier(parser);
        }
        return repository.findAll();
    }

    private void scrapeOneSupplier(SupplierPriceParser parser) {
        Optional<Supplier> supplier = supplierRepository.findByCountryAndName(parser.country(), parser.supplierName());
        if (supplier.isEmpty() || !supplier.get().isActive()) {
            log.warn("Skipping scrape for {}/{} - no active supplier row configured",
                    parser.country(), parser.supplierName());
            return;
        }
        String html;
        try {
            html = fetcher.fetch(supplier.get().getPriceUrl());
        } catch (SupplierPageFetchException e) {
            log.warn("Scrape failed for {}: {}", parser.supplierName(), e.getMessage());
            return;
        }
        Document doc = Jsoup.parse(html, supplier.get().getPriceUrl());
        List<ScrapedOffer> offers = parser.parse(doc);
        if (offers.isEmpty()) {
            log.warn("Scrape for {} returned no offers - page structure may have changed", parser.supplierName());
            return;
        }
        for (ScrapedOffer offer : offers) {
            upsert(parser.supplierName(), parser.country(), offer);
        }
    }

    private void upsert(String supplierName, CountryCode country, ScrapedOffer offer) {
        Optional<EnergyPackage> existing = repository.findBySupplierNameAndPackageName(supplierName, offer.packageName());
        if (existing.isEmpty()) {
            repository.save(new EnergyPackage(offer.packageName(), supplierName, country,
                    offer.pricePerKwh(), offer.marginPerKwh(), PackageSource.SCRAPED));
        } else if (existing.get().getSource() == PackageSource.SCRAPED) {
            existing.get().applyScrapedPrice(offer.pricePerKwh(), offer.marginPerKwh());
        } else {
            log.warn("Scraped offer '{}' / '{}' matches a MANUAL package by name - leaving it untouched",
                    supplierName, offer.packageName());
        }
    }
}
