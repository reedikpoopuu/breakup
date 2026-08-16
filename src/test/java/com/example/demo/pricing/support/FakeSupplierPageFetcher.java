package com.example.demo.pricing.support;

import com.example.demo.pricing.scrape.SupplierPageFetchException;
import com.example.demo.pricing.scrape.SupplierPageFetcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Stands in for {@code RestClientSupplierPageFetcher} in tests - serves the same
 * on-disk fixtures the parser unit tests use instead of making a real network call,
 * so {@code EnergyPackageService.runScraper()} can be exercised end-to-end (real
 * parsing + real upsert) inside a full {@code @SpringBootTest} without depending on
 * elenger.ee/alexela.ee/virsi.lv actually being reachable during a build.
 */
public class FakeSupplierPageFetcher implements SupplierPageFetcher {

    private static final Map<String, String> FIXTURE_BY_URL = Map.of(
            "https://elenger.ee/kodukliendile/elekter/", "/scrape-fixtures/elenger.html",
            "https://www.alexela.ee/et/elekter", "/scrape-fixtures/alexela.html",
            "https://www.virsi.lv/lv/privatpersonam/elektriba/elektriba", "/scrape-fixtures/virsi.html"
    );

    @Override
    public String fetch(String url) {
        String resource = FIXTURE_BY_URL.get(url);
        if (resource == null) {
            throw new SupplierPageFetchException(url, new IllegalStateException("no fixture registered for " + url));
        }
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SupplierPageFetchException(url, e);
        }
    }
}
