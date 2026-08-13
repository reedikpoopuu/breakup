package com.example.demo.auth;

/**
 * Thrown when Smart-ID relying-party credentials (issued by SK ID Solutions) are not
 * yet available, so the real REST integration cannot be called. See ARCH_SPEC.md
 * section 2.5 - this keeps the failure typed and predictable rather than surfacing a
 * raw connection error once wired to the real endpoint.
 */
public class SmartIdNotConfiguredException extends RuntimeException {

    public SmartIdNotConfiguredException() {
        super("Smart-ID relying-party credentials are not configured");
    }
}
