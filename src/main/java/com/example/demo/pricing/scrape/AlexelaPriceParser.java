package com.example.demo.pricing.scrape;

import com.example.demo.common.CountryCode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * https://www.alexela.ee/et/elekter - verified structure (2026-08): each offer is a
 * {@code .package-cards__card} with a {@code .package-cards__title} and a
 * {@code .package-cards__details} list of {@code .package-cards__info} rows, each a
 * label {@code <span>} then a value {@code <span>} (e.g. "Päeva hind" / "12,6  s/kWh",
 * "Marginaal" / "0,46  s/kWh", "Kuutasu" / "2,02 €"). Packages vary: some quote a flat
 * or day/night price, some only a margin (market-linked - no live spot feed is wired
 * into this scraper, see {@code datahub.NordPoolClient} separately, so pricePerKwh
 * mirrors the margin for those). "Kuutasu" (monthly fee) isn't a per-kWh figure and
 * isn't captured - {@link com.example.demo.pricing.EnergyPackage} has no field for it.
 */
@Component
public class AlexelaPriceParser implements SupplierPriceParser {

    @Override
    public String supplierName() {
        return "Alexela";
    }

    @Override
    public CountryCode country() {
        return CountryCode.EE;
    }

    @Override
    public List<ScrapedOffer> parse(Document doc) {
        List<ScrapedOffer> offers = new ArrayList<>();
        for (Element card : doc.select(".package-cards__card")) {
            String name = card.select(".package-cards__title").text();
            if (name.isBlank()) {
                continue;
            }
            Map<String, String> info = new LinkedHashMap<>();
            for (Element row : card.select(".package-cards__details .package-cards__info")) {
                Elements spans = row.select("span");
                if (spans.size() < 2) {
                    continue;
                }
                info.put(spans.first().text().trim().toLowerCase(), spans.last().text().trim());
            }

            BigDecimal margin = info.entrySet().stream()
                    .filter(e -> e.getKey().contains("marginaal"))
                    .findFirst()
                    .flatMap(e -> PriceText.parseEurosPerKwh(e.getValue()))
                    .orElse(BigDecimal.ZERO);

            List<BigDecimal> priceReadings = info.entrySet().stream()
                    .filter(e -> e.getKey().contains("hind") && !e.getKey().contains("kuutasu"))
                    .map(e -> PriceText.parseEurosPerKwh(e.getValue()))
                    .flatMap(Optional::stream)
                    .toList();

            BigDecimal price;
            if (!priceReadings.isEmpty()) {
                BigDecimal sum = priceReadings.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                price = sum.divide(BigDecimal.valueOf(priceReadings.size()), 4, java.math.RoundingMode.HALF_UP);
            } else if (margin.compareTo(BigDecimal.ZERO) > 0) {
                price = margin;
            } else {
                continue;
            }
            offers.add(new ScrapedOffer(name, price, margin));
        }
        return offers;
    }
}
