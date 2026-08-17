package com.example.demo.datahub;

import com.example.demo.common.CountryCode;

/**
 * Thrown when {@code customerPermission} is false for an adapter whose real API has no
 * per-request consent field to forward that to (ESO - consent is a separate,
 * out-of-band "access right" resource there, not a request parameter). Refusing
 * client-side is the only honest option: sending the order anyway would assert access
 * the caller just told us they don't have.
 */
public class DataHubConsentRequiredException extends RuntimeException {

    public DataHubConsentRequiredException(CountryCode country) {
        super("Customer permission is required to fetch consumption data for " + country
                + " - the caller must hold the client's consent before calling this adapter");
    }
}
