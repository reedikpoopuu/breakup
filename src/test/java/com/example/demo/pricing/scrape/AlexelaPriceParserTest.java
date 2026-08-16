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

class AlexelaPriceParserTest {

    private final AlexelaPriceParser parser = new AlexelaPriceParser();

    private Document fixture() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/scrape-fixtures/alexela.html")) {
            return Jsoup.parse(in, "UTF-8", "https://www.alexela.ee/et/elekter");
        }
    }

    @Test
    void identifiesItself() {
        assertThat(parser.supplierName()).isEqualTo("Alexela");
        assertThat(parser.country()).isEqualTo(CountryCode.EE);
    }

    @Test
    void parsesMarginOnlyDayNightAndAllowanceCardsFromTheRealPageStructure() throws IOException {
        List<ScrapedOffer> offers = parser.parse(fixture());

        assertThat(offers).extracting(ScrapedOffer::packageName)
                .containsExactlyInAnyOrder("Vali ise", "Paindlik fiks", "Kindel maht");

        ScrapedOffer marginOnly = byName(offers, "Vali ise");
        assertThat(marginOnly.marginPerKwh()).isEqualByComparingTo(new BigDecimal("0.0046"));
        assertThat(marginOnly.pricePerKwh()).isEqualByComparingTo(marginOnly.marginPerKwh());

        ScrapedOffer dayNight = byName(offers, "Paindlik fiks");
        assertThat(dayNight.pricePerKwh()).isEqualByComparingTo(new BigDecimal("0.1260"));
        assertThat(dayNight.marginPerKwh()).isEqualByComparingTo(BigDecimal.ZERO);

        ScrapedOffer allowance = byName(offers, "Kindel maht");
        assertThat(allowance.pricePerKwh()).isEqualByComparingTo(new BigDecimal("0.0775"));
        assertThat(allowance.marginPerKwh()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static ScrapedOffer byName(List<ScrapedOffer> offers, String name) {
        return offers.stream().filter(o -> o.packageName().equals(name)).findFirst().orElseThrow();
    }
}
