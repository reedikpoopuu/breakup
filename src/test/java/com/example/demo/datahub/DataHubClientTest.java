package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies {@link DataHubClientResolver} selects the right per-country adapter (ARCH_SPEC.md section 3.2). */
class DataHubClientTest {

    private static class StubClient implements DataHubClient {
        private final CountryCode country;

        StubClient(CountryCode country) {
            this.country = country;
        }

        @Override
        public CountryCode getCountry() {
            return country;
        }

        @Override
        public List<ConsumptionRecord> fetchConsumption(String eicCode, LocalDate from, LocalDate to) {
            return List.of();
        }
    }

    private final StubClient ee = new StubClient(CountryCode.EE);
    private final StubClient lv = new StubClient(CountryCode.LV);
    private final StubClient lt = new StubClient(CountryCode.LT);
    private final DataHubClientResolver resolver = new DataHubClientResolver(List.of(ee, lv, lt));

    @Test
    void resolvesEstonianAdapter() {
        assertThat(resolver.resolve(CountryCode.EE)).isSameAs(ee);
    }

    @Test
    void resolvesLatvianAdapter() {
        assertThat(resolver.resolve(CountryCode.LV)).isSameAs(lv);
    }

    @Test
    void resolvesLithuanianAdapter() {
        assertThat(resolver.resolve(CountryCode.LT)).isSameAs(lt);
    }

    @Test
    void throwsWhenNoAdapterRegisteredForCountry() {
        DataHubClientResolver partial = new DataHubClientResolver(List.of(ee, lv));

        assertThatThrownBy(() -> partial.resolve(CountryCode.LT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eachAdapterReportsItsOwnCountry() {
        assertThat(ee.getCountry()).isEqualTo(CountryCode.EE);
        assertThat(lv.getCountry()).isEqualTo(CountryCode.LV);
    }

    @Test
    void resolverIsBuiltFromAllRegisteredClients() {
        assertThat(resolver.resolve(CountryCode.EE).getCountry()).isEqualTo(CountryCode.EE);
        assertThat(resolver.resolve(CountryCode.LV).getCountry()).isEqualTo(CountryCode.LV);
        assertThat(resolver.resolve(CountryCode.LT).getCountry()).isEqualTo(CountryCode.LT);
    }
}
