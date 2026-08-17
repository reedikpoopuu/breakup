package com.example.demo.datahub;

import com.example.demo.common.CountryCode;

import java.time.LocalDate;
import java.util.List;

/**
 * One adapter interface, one implementation per country's national metering-data
 * platform (ARCH_SPEC.md section 3.2). Each country has a materially different
 * auth/protocol, so implementations share no HTTP client config beyond the
 * {@code RestClient} they each build internally.
 * <p>
 * {@code customerEic} and {@code objectEic} are separate identifiers - the client
 * (person/company being billed) and the metering point - because STEP's real
 * {@code GetObjectConsumption} message requires both plus an explicit consent flag;
 * see {@link StepClient}. Estfeed/ESO only need the metering point and ignore the
 * other two parameters rather than the interface silently asserting consent that was
 * never actually obtained.
 */
public interface DataHubClient {

    CountryCode getCountry();

    /**
     * @param customerEic        the client's own EIC - required by STEP, ignored by
     *                            adapters whose real API has no such field
     * @param objectEic           the metering point's EIC - what Estfeed calls
     *                            {@code meteringPointEic}
     * @param customerPermission  true only if the caller actually holds the client's
     *                            consent to access this data; adapters that enforce
     *                            consent (STEP) reject the call otherwise
     * @throws DataHubNotConfiguredException if this adapter's credentials are not yet
     *                                        set - all three national DataHubs are pre-credential as of this build.
     */
    List<ConsumptionRecord> fetchConsumption(String customerEic, String objectEic, boolean customerPermission,
                                              LocalDate from, LocalDate to);
}
