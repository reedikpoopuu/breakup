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
 * see {@link StepClient}. Estfeed only needs the metering point and ignores the other
 * two parameters. ESO's real API has no per-request consent field at all - consent is
 * a separate, out-of-band resource there - so {@link EsoClient} enforces {@code
 * customerPermission} as a client-side guard instead ({@link DataHubConsentRequiredException}).
 * Either way, no adapter here silently asserts consent that was never actually obtained.
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
     *                            consent (STEP, ESO) reject the call otherwise
     * @throws DataHubNotConfiguredException if this adapter's credentials are not yet
     *                                        set - all three national DataHubs are pre-credential as of this build.
     */
    List<ConsumptionRecord> fetchConsumption(String customerEic, String objectEic, boolean customerPermission,
                                              LocalDate from, LocalDate to);
}
