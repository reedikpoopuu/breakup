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

class VirsiPriceParserTest {

    private final VirsiPriceParser parser = new VirsiPriceParser();

    private Document fixture() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/scrape-fixtures/virsi.html")) {
            return Jsoup.parse(in, "UTF-8", "https://www.virsi.lv/lv/privatpersonam/elektriba/elektriba");
        }
    }

    @Test
    void identifiesItself() {
        assertThat(parser.supplierName()).isEqualTo("Virši");
        assertThat(parser.country()).isEqualTo(CountryCode.LV);
    }

    @Test
    void parsesFixedPriceCardsFromTheRealPageStructure() throws IOException {
        List<ScrapedOffer> offers = parser.parse(fixture());

        assertThat(offers).hasSize(3);
        assertThat(offers).extracting(ScrapedOffer::packageName)
                .containsExactlyInAnyOrder(
                        "Varenais komplekts ar fiksēto elektrību 12M",
                        "Varenais komplekts ar fiksēto elektrību 24M",
                        "Fiksētā elektrība 12M");
        assertThat(offers).allSatisfy(o -> assertThat(o.marginPerKwh()).isEqualByComparingTo(BigDecimal.ZERO));

        ScrapedOffer twelveMonth = offers.stream()
                .filter(o -> o.packageName().equals("Varenais komplekts ar fiksēto elektrību 12M"))
                .findFirst().orElseThrow();
        assertThat(twelveMonth.pricePerKwh()).isEqualByComparingTo(new BigDecimal("0.1350"));

        ScrapedOffer twentyFourMonth = offers.stream()
                .filter(o -> o.packageName().equals("Varenais komplekts ar fiksēto elektrību 24M"))
                .findFirst().orElseThrow();
        assertThat(twentyFourMonth.pricePerKwh()).isEqualByComparingTo(new BigDecimal("0.1300"));
    }
}
