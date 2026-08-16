package com.example.demo.pricing.scrape;

import com.example.demo.common.CountryCode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * https://elenger.ee/kodukliendile/elekter/ - verified structure (2026-08): each offer
 * is a {@code .c-columns__item} with a non-blank {@code <h3>} title and a {@code <li>}
 * reading e.g. "Hind (km-ga): alates 12,67 senti/kWh" (fixed) or "Hind (km-ga):
 * börsihind + 0,67 senti/kWh" (market-linked). No live spot-price feed is wired into
 * this scraper (see {@code datahub.NordPoolClient} for that, separately) - for
 * market-linked offers, pricePerKwh mirrors the margin since that's the only real
 * number the page discloses.
 */
@Component
public class ElengerPriceParser implements SupplierPriceParser {

    @Override
    public String supplierName() {
        return "Elenger";
    }

    @Override
    public CountryCode country() {
        return CountryCode.EE;
    }

    @Override
    public List<ScrapedOffer> parse(Document doc) {
        List<ScrapedOffer> offers = new ArrayList<>();
        for (Element item : doc.select(".c-columns__item")) {
            String name = item.select("h3").stream()
                    .map(Element::text)
                    .filter(text -> !text.isBlank())
                    .findFirst()
                    .orElse(null);
            if (name == null) {
                continue;
            }
            Elements priceLines = item.select("li");
            String priceLine = priceLines.stream()
                    .map(Element::text)
                    .filter(text -> text.toLowerCase().contains("hind") && text.toLowerCase().contains("kwh"))
                    .findFirst()
                    .orElse(null);
            if (priceLine == null) {
                continue;
            }
            Optional<BigDecimal> parsed = PriceText.parseEurosPerKwh(priceLine);
            if (parsed.isEmpty()) {
                continue;
            }
            BigDecimal euros = parsed.get();
            boolean marketLinked = priceLine.toLowerCase().contains("börsihind");
            BigDecimal margin = marketLinked ? euros : BigDecimal.ZERO;
            offers.add(new ScrapedOffer(name, euros, margin));
        }
        return offers;
    }
}
