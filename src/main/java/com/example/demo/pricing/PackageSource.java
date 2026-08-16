package com.example.demo.pricing;

/** Where an {@link EnergyPackage} row's price/margin come from. */
public enum PackageSource {
    /** Written and maintained by an admin - the scraper never touches these. */
    MANUAL,
    /** Last written by {@code EnergyPackageService.runScraper()}. */
    SCRAPED
}
