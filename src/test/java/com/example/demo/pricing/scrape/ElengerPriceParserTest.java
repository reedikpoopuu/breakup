package com.example.demo.pricing.scrape;

import com.example.demo.common.CountryCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ElengerPriceParserTest {

    private final ElengerPriceParser parser = new ElengerPriceParser();

    private Document fixture() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/scrape-fixtures/elenger.html")) {
            return Jsoup.parse(in, "UTF-8", "https://elenger.ee/kodukliendile/elekter/");
        }
    }

    @Test
    void identifiesItself() {
        assertThat(parser.supplierName()).isEqualTo("Elenger");
        assertThat(parser.country()).isEqualTo(CountryCode.EE);
    }

    @Test
    void parsesFixedAndMarketLinkedOffersFromTheRealPageStructure() throws IOException {
        List<ScrapedOffer> offers = parser.parse(fixture());

        assertThat(offers).hasSize(2);
        assertThat(offers).extracting(ScrapedOffer::packageName)
                .containsExactlyInAnyOrder("KINDEL PAKETT", "MUUTUVHINNAGA PAKETT");

        ScrapedOffer fixed = offers.stream().filter(o -> o.packageName().equals("KINDEL PAKETT")).findFirst().orElseThrow();
        assertThat(fixed.pricePerKwh()).isEqualByComparingTo(new BigDecimal("0.1267"));
        assertThat(fixed.marginPerKwh()).isEqualByComparingTo(BigDecimal.ZERO);

        ScrapedOffer marketLinked = offers.stream().filter(o -> o.packageName().equals("MUUTUVHINNAGA PAKETT")).findFirst().orElseThrow();
        assertThat(marketLinked.marginPerKwh()).isEqualByComparingTo(new BigDecimal("0.0067"));
        assertThat(marketLinked.pricePerKwh()).isEqualByComparingTo(marketLinked.marginPerKwh());
    }
}
