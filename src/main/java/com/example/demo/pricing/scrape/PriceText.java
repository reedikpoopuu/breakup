package com.example.demo.pricing.scrape;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared number parsing for supplier price pages - Estonian/Latvian pages use a comma decimal separator. */
final class PriceText {

    private static final Pattern DECIMAL = Pattern.compile("(\\d+(?:[.,]\\d+)?)");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private PriceText() {
    }

    /** First decimal number in the text, comma or dot as separator (e.g. "12,67 senti/kWh" -&gt; 12.67). */
    static Optional<BigDecimal> extractDecimal(String text) {
        Matcher matcher = DECIMAL.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(matcher.group(1).replace(',', '.')));
    }

    /** cents/kWh (however the site spells it - senti, s/kWh, c/kWh) -&gt; euros/kWh, 4dp. */
    static BigDecimal centsToEuros(BigDecimal cents) {
        return cents.divide(HUNDRED, 4, RoundingMode.HALF_UP);
    }

    /**
     * First number in text, converted to euros/kWh - detects whether the site already
     * quoted euros ("0,13500 €/kWh") or cents ("12,6 s/kWh", "12,67 senti/kWh").
     */
    static Optional<BigDecimal> parseEurosPerKwh(String text) {
        Optional<BigDecimal> number = extractDecimal(text);
        if (number.isEmpty()) {
            return Optional.empty();
        }
        String lower = text.toLowerCase();
        boolean alreadyEuros = lower.contains("€/kwh") || lower.contains("eur/kwh");
        return Optional.of(alreadyEuros ? number.get().setScale(4, RoundingMode.HALF_UP) : centsToEuros(number.get()));
    }
}
