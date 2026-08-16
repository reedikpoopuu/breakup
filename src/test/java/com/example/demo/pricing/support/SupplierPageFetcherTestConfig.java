package com.example.demo.pricing.support;

import com.example.demo.pricing.scrape.SupplierPageFetcher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class SupplierPageFetcherTestConfig {

    @Bean
    @Primary
    public SupplierPageFetcher supplierPageFetcher() {
        return new FakeSupplierPageFetcher();
    }
}
