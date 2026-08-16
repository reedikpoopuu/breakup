package com.example.demo.pricing.scrape;

/** A supplier's price page couldn't be fetched (network error, non-2xx, timeout, ...). */
public class SupplierPageFetchException extends RuntimeException {

    public SupplierPageFetchException(String url, Throwable cause) {
        super("Could not fetch " + url, cause);
    }
}
