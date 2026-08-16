package com.example.demo.pricing.scrape;

import java.math.BigDecimal;

/** One package a {@link SupplierPriceParser} found on a supplier's public price page. */
public record ScrapedOffer(String packageName, BigDecimal pricePerKwh, BigDecimal marginPerKwh) {
}
