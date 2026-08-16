package com.example.demo.pricing;

/** A SCRAPED package can't be hand-edited - the next scrape run would just overwrite it. */
public class EnergyPackageNotEditableException extends RuntimeException {

    public EnergyPackageNotEditableException(Long id) {
        super("Energy package " + id + " was scraped, not manually added - it can't be edited directly. "
                + "Hide it instead (visibility toggle) if it's wrong, or wait for the next scrape.");
    }
}
