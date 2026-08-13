package com.example.demo.auth;

/**
 * Returned to the browser immediately after starting a Smart-ID authentication
 * challenge: the session to poll and the 4-digit verification code to display so the
 * user can confirm it matches their device's Smart-ID app prompt.
 */
public record SmartIdAuthStart(String sessionId, String verificationCode) {
}
