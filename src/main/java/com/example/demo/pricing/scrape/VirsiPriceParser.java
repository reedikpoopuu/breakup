package com.example.demo.pricing.scrape;

import com.example.demo.common.CountryCode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * https://www.virsi.lv/lv/privatpersonam/elektriba/elektriba - verified structure
 * (2026-08): each offer is a {@code div[data-product]} with a
 * {@code .product-title h3} name and {@code .product-body .product-advantage} rows,
 * each a {@code .advantage-text} label and {@code .advantage-value} value (e.g.
 * "Fiksētā elektrības cena" / "0,13500 €/kWh", or "Nord Pool biržas cena + Tirgotāja
 * uzcenojums" / "0,00920 €/kWh" for market-linked offers - only the markup is
 * disclosed there, so pricePerKwh mirrors it per the same convention used for the
 * other market-linked suppliers; see {@code datahub.NordPoolClient} for the live spot
 * feed this scraper doesn't integrate). "Mēneša maksa" (monthly fee) isn't captured -
 * {@link com.example.demo.pricing.EnergyPackage} has no field for it.
 */
@Component
public class VirsiPriceParser implements SupplierPriceParser {

    @Override
    public String supplierName() {
        return "Virši";
    }

    @Override
    public CountryCode country() {
        return CountryCode.LV;
    }

    @Override
    public List<ScrapedOffer> parse(Document doc) {
        List<ScrapedOffer> offers = new ArrayList<>();
        for (Element card : doc.select("div[data-product]")) {
            String name = card.select(".product-title h3").text();
            if (name.isBlank()) {
                continue;
            }
            for (Element advantage : card.select(".product-body .product-advantage")) {
                String label = advantage.select(".advantage-text").text().toLowerCase();
                String value = advantage.select(".advantage-value").text();
                if (!label.contains("cena") || !value.toLowerCase().contains("kwh")) {
                    continue;
                }
                Optional<BigDecimal> parsed = PriceText.parseEurosPerKwh(value);
                if (parsed.isEmpty()) {
                    continue;
                }
                boolean marketLinked = label.contains("nord pool");
                BigDecimal euros = parsed.get();
                BigDecimal margin = marketLinked ? euros : BigDecimal.ZERO;
                offers.add(new ScrapedOffer(name, euros, margin));
                break;
            }
        }
        return offers;
    }
}
