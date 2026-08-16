package com.example.demo.pricing.scrape;

/** The network seam of price scraping - swapped for a fixture-backed fake in tests. */
public interface SupplierPageFetcher {

    /** @throws SupplierPageFetchException on any network error or non-2xx response. */
    String fetch(String url);
}
