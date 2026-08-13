package com.example.demo.auth;

/**
 * Result of polling a Smart-ID session. {@code smartIdIdentity} and {@code displayName}
 * are populated only when {@code status == COMPLETE}, extracted from the validated
 * Smart-ID certificate chain.
 */
public record SmartIdAuthResult(SmartIdAuthStatus status, String smartIdIdentity, String displayName) {

    public static SmartIdAuthResult running() {
        return new SmartIdAuthResult(SmartIdAuthStatus.RUNNING, null, null);
    }

    public static SmartIdAuthResult failed() {
        return new SmartIdAuthResult(SmartIdAuthStatus.FAILED, null, null);
    }

    public static SmartIdAuthResult complete(String smartIdIdentity, String displayName) {
        return new SmartIdAuthResult(SmartIdAuthStatus.COMPLETE, smartIdIdentity, displayName);
    }
}
