package com.example.demo.auth;

/**
 * Relying-party integration with Smart-ID (authentication endpoint). Kept behind an
 * interface so the real REST wiring can be dropped in once SK ID Solutions issues
 * relying-party credentials, without touching {@link SmartIdController} or the
 * token-issuing code. See ARCH_SPEC.md section 2.3/2.5.
 */
public interface SmartIdService {

    /**
     * Starts an authentication session for the given Smart-ID identity (semantics
     * identifier / national ID number + country, e.g. {@code EE-38507030022}).
     */
    SmartIdAuthStart startAuthentication(String smartIdIdentity);

    /** Polls session status; frontend calls this repeatedly until COMPLETE or FAILED. */
    SmartIdAuthResult pollAuthentication(String sessionId);
}
