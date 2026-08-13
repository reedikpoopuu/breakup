package com.example.demo.datahub;

import com.example.demo.common.CountryCode;

import java.time.LocalDate;
import java.util.List;

/**
 * One adapter interface, one implementation per country's national metering-data
 * platform (ARCH_SPEC.md section 3.2). Each country has a materially different
 * auth/protocol, so implementations share no HTTP client config beyond the
 * {@code RestClient} they each build internally.
 */
public interface DataHubClient {

    CountryCode getCountry();

    /**
     * @throws DataHubNotConfiguredException if this adapter's credentials are not yet
     *                                        set - all three national DataHubs are pre-credential as of this build.
     */
    List<ConsumptionRecord> fetchConsumption(String eicCode, LocalDate from, LocalDate to);
}
