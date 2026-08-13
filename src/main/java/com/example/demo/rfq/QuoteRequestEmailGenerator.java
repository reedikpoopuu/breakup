package com.example.demo.rfq;

import com.example.demo.common.CountryCode;
import com.example.demo.supplier.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Generates the RFQ email sent to a supplier, localized to the supplier's country
 * language (et/lv/lt) - matches the translated request-letter copy already designed in
 * {@code energy-switcher.html} (ARCH_SPEC.md section 1.4).
 */
@Component
public class QuoteRequestEmailGenerator {

    private record Template(String subject, String body) {
    }

    private static final Map<CountryCode, Template> TEMPLATES = Map.of(
            CountryCode.EE, new Template(
                    "Hinnapäring elektrienergia tarnimiseks – %s",
                    "Tere,\n\nPalume Teil esitada hinnapakkumine elektrienergia tarnimiseks ettevõttele %s. "
                            + "Palume vastata 5 tööpäeva jooksul.\n\nLugupidamisega,\nEasyBreak"),
            CountryCode.LV, new Template(
                    "Cenu pieprasījums elektroenerģijas piegādei – %s",
                    "Labdien,\n\nLūdzam iesniegt cenu piedāvājumu elektroenerģijas piegādei uzņēmumam %s. "
                            + "Lūdzam atbildēt 5 darba dienu laikā.\n\nAr cieņu,\nEasyBreak"),
            CountryCode.LT, new Template(
                    "Kainos pasiūlymo prašymas elektros energijos tiekimui – %s",
                    "Laba diena,\n\nPrašome pateikti kainos pasiūlymą elektros energijos tiekimui įmonei %s. "
                            + "Prašome atsakyti per 5 darbo dienas.\n\nPagarbiai,\nEasyBreak")
    );

    private final String mailDomain;

    public QuoteRequestEmailGenerator(@Value("${app.rfq.mail-domain}") String mailDomain) {
        this.mailDomain = mailDomain;
    }

    public RfqEmailContent generate(Supplier supplier, String companySlug, String companyName) {
        Template template = TEMPLATES.get(supplier.getCountry());
        if (template == null) {
            throw new IllegalArgumentException("No RFQ template for " + supplier.getCountry());
        }
        String from = companySlug + "@" + mailDomain;
        return new RfqEmailContent(
                from,
                supplier.getRfqEmail(),
                template.subject().formatted(companyName),
                template.body().formatted(companyName));
    }
}
