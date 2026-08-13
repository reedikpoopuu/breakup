package com.example.demo.rfq;

import com.example.demo.common.CountryCode;
import com.example.demo.supplier.Supplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteRequestEmailGeneratorTest {

    private final QuoteRequestEmailGenerator generator = new QuoteRequestEmailGenerator("quote.easybreak.com");

    @Test
    void estonianSupplierGetsEstonianEmail() {
        Supplier enefitEe = new Supplier(CountryCode.EE, "Enefit", "arikliendid@enefit.ee", "https://enefit.ee");

        RfqEmailContent email = generator.generate(enefitEe, "nordicwoods", "Nordic Woods OÜ");

        assertThat(email.from()).isEqualTo("nordicwoods@quote.easybreak.com");
        assertThat(email.to()).isEqualTo("arikliendid@enefit.ee");
        assertThat(email.subject()).contains("Hinnapäring").contains("Nordic Woods OÜ");
        assertThat(email.body()).contains("Palume").contains("Nordic Woods OÜ");
    }

    @Test
    void latvianSupplierGetsLatvianEmail() {
        Supplier elektrumLv = new Supplier(CountryCode.LV, "Elektrum", "klientu.serviss@elektrum.lv",
                "https://elektrum.lv");

        RfqEmailContent email = generator.generate(elektrumLv, "balticwood", "Baltic Wood SIA");

        assertThat(email.subject()).contains("Cenu pieprasījums").contains("Baltic Wood SIA");
        assertThat(email.body()).contains("Lūdzam").contains("Baltic Wood SIA");
    }

    @Test
    void lithuanianSupplierGetsLithuanianEmail() {
        Supplier ignitisLt = new Supplier(CountryCode.LT, "Ignitis", "info@ignitis.lt", "https://ignitis.lt");

        RfqEmailContent email = generator.generate(ignitisLt, "medienuk", "Medienuk UAB");

        assertThat(email.subject()).contains("Kainos pasiūlymo").contains("Medienuk UAB");
        assertThat(email.body()).contains("Prašome").contains("Medienuk UAB");
    }

    @Test
    void fromAddressUsesConfiguredMailDomain() {
        QuoteRequestEmailGenerator customDomain = new QuoteRequestEmailGenerator("rfq.example.com");
        Supplier supplier = new Supplier(CountryCode.EE, "Alexela", "ariklient@alexela.ee", "https://alexela.ee");

        RfqEmailContent email = customDomain.generate(supplier, "acme", "Acme OÜ");

        assertThat(email.from()).isEqualTo("acme@rfq.example.com");
    }
}
