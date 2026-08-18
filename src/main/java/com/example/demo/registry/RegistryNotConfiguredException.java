package com.example.demo.registry;

import com.example.demo.common.CountryCode;

/**
 * Thrown instead of a raw HTTP error when a national business-registry adapter's
 * credentials are absent, so representative-rights verification degrades predictably
 * in dev/H2 until each country's credentials arrive - mirrors {@code
 * DataHubNotConfiguredException}.
 */
public class RegistryNotConfiguredException extends RuntimeException {

    public RegistryNotConfiguredException(CountryCode country) {
        super("Business registry adapter for " + country + " is not configured");
    }
}
