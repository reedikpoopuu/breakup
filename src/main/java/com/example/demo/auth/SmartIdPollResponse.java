package com.example.demo.auth;

/** Frontend polls {@code GET /api/auth/smart-id/poll/{sessionId}} until {@code status == COMPLETE} and a token is present. */
public record SmartIdPollResponse(SmartIdAuthStatus status, String token, Role role, String displayName) {

    public static SmartIdPollResponse pending(SmartIdAuthStatus status) {
        return new SmartIdPollResponse(status, null, null, null);
    }

    public static SmartIdPollResponse issued(String token, Role role, String displayName) {
        return new SmartIdPollResponse(SmartIdAuthStatus.COMPLETE, token, role, displayName);
    }
}
