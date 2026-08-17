package com.example.demo.contract;

import com.example.demo.common.CountryCode;

/**
 * A company registry code found near its country's own label term (e.g. Estonian
 * "Registrikood"). Called a "candidate", not a confirmed result, deliberately - see
 * {@link ContractFieldExtractor}'s javadoc for why.
 */
public record RegistryCodeCandidate(CountryCode country, String code) {
}
