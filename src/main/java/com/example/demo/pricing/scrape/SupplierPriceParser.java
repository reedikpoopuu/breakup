package com.example.demo.pricing.scrape;

import com.example.demo.common.CountryCode;
import org.jsoup.nodes.Document;

import java.util.List;

/**
 * Extracts package offers from one supplier's public price page. Implemented per
 * supplier because every site's markup and pricing vocabulary is different (see the
 * individual implementations for the real page structure each was written against) -
 * there's no generic HTML pattern that works across them. {@link #supplierName()} and
 * {@link #country()} identify which {@code Supplier} row (seeded by
 * {@code SupplierSeeder}) supplies the URL to fetch and parse.
 */
public interface SupplierPriceParser {

    String supplierName();

    CountryCode country();

    /** Pure and network-free: {@code EnergyPackageService} fetches the HTML, this only parses it. */
    List<ScrapedOffer> parse(Document doc);
}
