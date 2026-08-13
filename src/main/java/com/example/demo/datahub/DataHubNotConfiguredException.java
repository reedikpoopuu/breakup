package com.example.demo.datahub;

import com.example.demo.common.CountryCode;

/**
 * Thrown instead of a raw HTTP error when a national DataHub adapter's credentials are
 * absent, so the consumption-fetch flow degrades predictably in dev/H2 until each
 * country's credentials arrive (ARCH_SPEC.md section 3.4).
 */
public class DataHubNotConfiguredException extends RuntimeException {

    public DataHubNotConfiguredException(CountryCode country) {
        super("DataHub adapter for " + country + " is not configured");
    }
}
