package com.example.demo.pricing;

import com.example.demo.common.CountryCode;
import com.example.demo.pricing.scrape.ScrapedOffer;
import com.example.demo.pricing.scrape.SupplierPageFetchException;
import com.example.demo.pricing.scrape.SupplierPageFetcher;
import com.example.demo.pricing.scrape.SupplierPriceParser;
import com.example.demo.supplier.Supplier;
import com.example.demo.supplier.SupplierRepository;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnergyPackageServiceTest {

    private final EnergyPackageRepository packages = mock(EnergyPackageRepository.class);
    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final SupplierPageFetcher fetcher = mock(SupplierPageFetcher.class);

    private static SupplierPriceParser parserReturning(String supplierName, CountryCode country, List<ScrapedOffer> offers) {
        return new SupplierPriceParser() {
            @Override
            public String supplierName() {
                return supplierName;
            }

            @Override
            public CountryCode country() {
                return country;
            }

            @Override
            public List<ScrapedOffer> parse(Document doc) {
                return offers;
            }
        };
    }

    @Test
    void createsANewScrapedRowWhenNoPackageWithThatNameExists() {
        Supplier supplier = new Supplier(CountryCode.EE, "Elenger", "x@elenger.ee", "https://elenger.ee/price");
        when(suppliers.findByCountryAndName(CountryCode.EE, "Elenger")).thenReturn(Optional.of(supplier));
        when(fetcher.fetch("https://elenger.ee/price")).thenReturn("<html></html>");
        when(packages.findBySupplierNameAndPackageName("Elenger", "KINDEL PAKETT")).thenReturn(Optional.empty());

        ScrapedOffer offer = new ScrapedOffer("KINDEL PAKETT", new BigDecimal("0.1267"), BigDecimal.ZERO);
        SupplierPriceParser parser = parserReturning("Elenger", CountryCode.EE, List.of(offer));
        EnergyPackageService service = new EnergyPackageService(packages, suppliers, List.of(parser), fetcher);

        service.runScraper();

        var captor = org.mockito.ArgumentCaptor.forClass(EnergyPackage.class);
        verify(packages).save(captor.capture());
        assertThat(captor.getValue().getPackageName()).isEqualTo("KINDEL PAKETT");
        assertThat(captor.getValue().getSource()).isEqualTo(PackageSource.SCRAPED);
        assertThat(captor.getValue().getPricePerKwh()).isEqualByComparingTo("0.1267");
    }

    @Test
    void updatesAnExistingScrapedRowInPlace() {
        Supplier supplier = new Supplier(CountryCode.EE, "Elenger", "x@elenger.ee", "https://elenger.ee/price");
        when(suppliers.findByCountryAndName(CountryCode.EE, "Elenger")).thenReturn(Optional.of(supplier));
        when(fetcher.fetch(any())).thenReturn("<html></html>");

        EnergyPackage existing = new EnergyPackage("KINDEL PAKETT", "Elenger", CountryCode.EE,
                new BigDecimal("0.1000"), BigDecimal.ZERO, PackageSource.SCRAPED);
        when(packages.findBySupplierNameAndPackageName("Elenger", "KINDEL PAKETT")).thenReturn(Optional.of(existing));

        ScrapedOffer offer = new ScrapedOffer("KINDEL PAKETT", new BigDecimal("0.1267"), BigDecimal.ZERO);
        SupplierPriceParser parser = parserReturning("Elenger", CountryCode.EE, List.of(offer));
        EnergyPackageService service = new EnergyPackageService(packages, suppliers, List.of(parser), fetcher);

        service.runScraper();

        assertThat(existing.getPricePerKwh()).isEqualByComparingTo("0.1267");
        verify(packages, never()).save(any());
    }

    @Test
    void neverOverwritesAManualPackageThatHappensToShareAName() {
        Supplier supplier = new Supplier(CountryCode.EE, "Elenger", "x@elenger.ee", "https://elenger.ee/price");
        when(suppliers.findByCountryAndName(CountryCode.EE, "Elenger")).thenReturn(Optional.of(supplier));
        when(fetcher.fetch(any())).thenReturn("<html></html>");

        EnergyPackage manual = new EnergyPackage("KINDEL PAKETT", "Elenger", CountryCode.EE,
                new BigDecimal("0.9999"), BigDecimal.ZERO, PackageSource.MANUAL);
        when(packages.findBySupplierNameAndPackageName("Elenger", "KINDEL PAKETT")).thenReturn(Optional.of(manual));

        ScrapedOffer offer = new ScrapedOffer("KINDEL PAKETT", new BigDecimal("0.1267"), BigDecimal.ZERO);
        SupplierPriceParser parser = parserReturning("Elenger", CountryCode.EE, List.of(offer));
        EnergyPackageService service = new EnergyPackageService(packages, suppliers, List.of(parser), fetcher);

        service.runScraper();

        assertThat(manual.getPricePerKwh()).isEqualByComparingTo("0.9999");
        verify(packages, never()).save(any());
    }

    @Test
    void skipsAParserWhenNoMatchingSupplierRowExists() {
        when(suppliers.findByCountryAndName(any(), any())).thenReturn(Optional.empty());
        SupplierPriceParser parser = parserReturning("Elenger", CountryCode.EE, List.of());
        EnergyPackageService service = new EnergyPackageService(packages, suppliers, List.of(parser), fetcher);

        service.runScraper();

        verify(fetcher, never()).fetch(any());
        verify(packages, never()).save(any());
    }

    @Test
    void skipsAParserWhenTheFetchFails() {
        Supplier supplier = new Supplier(CountryCode.EE, "Elenger", "x@elenger.ee", "https://elenger.ee/price");
        when(suppliers.findByCountryAndName(CountryCode.EE, "Elenger")).thenReturn(Optional.of(supplier));
        when(fetcher.fetch(any())).thenThrow(new SupplierPageFetchException("https://elenger.ee/price", new RuntimeException("boom")));

        SupplierPriceParser parser = parserReturning("Elenger", CountryCode.EE, List.of());
        EnergyPackageService service = new EnergyPackageService(packages, suppliers, List.of(parser), fetcher);

        service.runScraper();

        verify(packages, never()).save(any());
    }

    @Test
    void createSavesAManualPackage() {
        when(packages.save(any())).thenAnswer(inv -> inv.getArgument(0));
        EnergyPackageService service = new EnergyPackageService(packages, suppliers, List.of(), fetcher);

        EnergyPackageRequest request = new EnergyPackageRequest("Custom Deal", "Enefit", CountryCode.EE,
                new BigDecimal("0.15"), new BigDecimal("0.02"));
        EnergyPackage created = service.create(request);

        assertThat(created.getSource()).isEqualTo(PackageSource.MANUAL);
        assertThat(created.isVisible()).isTrue();
    }

    @Test
    void deleteThrowsWhenThePackageDoesNotExist() {
        when(packages.existsById(99L)).thenReturn(false);
        EnergyPackageService service = new EnergyPackageService(packages, suppliers, List.of(), fetcher);

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(EnergyPackageNotFoundException.class);
    }

    @Test
    void updateAppliesNewFieldsToAManualPackage() {
        EnergyPackage energyPackage = new EnergyPackage("Old Name", "Enefit", CountryCode.EE,
                new BigDecimal("0.10"), BigDecimal.ZERO, PackageSource.MANUAL);
        when(packages.findById(1L)).thenReturn(Optional.of(energyPackage));
        EnergyPackageService service = new EnergyPackageService(packages, suppliers, List.of(), fetcher);

        EnergyPackageRequest request = new EnergyPackageRequest("New Name", "Enefit", CountryCode.EE,
                new BigDecimal("0.20"), new BigDecimal("0.03"));
        EnergyPackage updated = service.update(1L, request);

        assertThat(updated.getPackageName()).isEqualTo("New Name");
        assertThat(updated.getPricePerKwh()).isEqualByComparingTo("0.20");
        assertThat(updated.getMarginPerKwh()).isEqualByComparingTo("0.03");
    }

    @Test
    void updateRefusesToTouchAScrapedPackage() {
        EnergyPackage energyPackage = new EnergyPackage("KINDEL PAKETT", "Elenger", CountryCode.EE,
                new BigDecimal("0.1267"), BigDecimal.ZERO, PackageSource.SCRAPED);
        when(packages.findById(1L)).thenReturn(Optional.of(energyPackage));
        EnergyPackageService service = new EnergyPackageService(packages, suppliers, List.of(), fetcher);

        EnergyPackageRequest request = new EnergyPackageRequest("Hacked", "Elenger", CountryCode.EE,
                new BigDecimal("0.01"), BigDecimal.ZERO);

        assertThatThrownBy(() -> service.update(1L, request)).isInstanceOf(EnergyPackageNotEditableException.class);
        assertThat(energyPackage.getPackageName()).isEqualTo("KINDEL PAKETT");
    }

    @Test
    void updateThrowsWhenThePackageDoesNotExist() {
        when(packages.findById(99L)).thenReturn(Optional.empty());
        EnergyPackageService service = new EnergyPackageService(packages, suppliers, List.of(), fetcher);

        EnergyPackageRequest request = new EnergyPackageRequest("X", "Y", CountryCode.EE, BigDecimal.ONE, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.update(99L, request)).isInstanceOf(EnergyPackageNotFoundException.class);
    }

    @Test
    void setVisibleTogglesTheFlag() {
        EnergyPackage energyPackage = new EnergyPackage("P", "S", CountryCode.EE, BigDecimal.ONE, BigDecimal.ZERO, PackageSource.MANUAL);
        when(packages.findById(1L)).thenReturn(Optional.of(energyPackage));
        EnergyPackageService service = new EnergyPackageService(packages, suppliers, List.of(), fetcher);

        service.setVisible(1L, false);

        assertThat(energyPackage.isVisible()).isFalse();
    }
}
