package com.example.demo.registry;

import com.example.demo.common.CountryCode;

import java.util.List;

/**
 * One adapter interface, one implementation per country's national business registry -
 * same shape as {@code DataHubClient} (ARCH_SPEC.md section 5 / ARCHITECTURE.md section
 * 3.2). {@link AriregisterEsindusClient} is the only implementation today (Estonian
 * e-Business Register / Ariregister); {@code docs/api-specs/business-registry/lv.md}
 * and {@code lt.md} are still awaiting a spec from the PM, so LV/LT companies cannot be
 * verified yet - {@link RepresentationRightsClientResolver} throws rather than silently
 * approving them.
 */
public interface RepresentationRightsClient {

    CountryCode getCountry();

    /**
     * @param registryCode the company's national business-registry code
     * @return everyone the registry lists as holding a right of representation over
     *         this company (board members, procurators, etc.) - empty if the company
     *         has no such entries or does not exist
     * @throws RegistryNotConfiguredException if this adapter's credentials are not yet
     *                                         set
     */
    List<CompanyRepresentative> fetchRepresentatives(String registryCode);
}
