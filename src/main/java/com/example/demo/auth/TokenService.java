package com.example.demo.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and validates signed, opaque bearer session tokens for Smart-ID logins
 * (ARCH_SPEC.md section 2.3 step 3 - "a signed session token ... with role embedded as
 * a claim"). No password/client-credentials grant exists; this is the only login path.
 * <p>
 * Revocation is a denylist ({@link RevokedTokenRepository}), not short-lived
 * access-tokens-plus-refresh-tokens - a deliberately simpler design, chosen because it
 * needs no change to the login flow or how the frontend carries the token, just one
 * extra lookup in {@link #validate}. Before this existed, there was no way to kill a
 * single compromised token short of rotating the signing secret, which logs out every
 * user at once, not just the one that leaked.
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private static final Duration TTL = Duration.ofHours(12);

    private final Mac mac;
    private final RevokedTokenRepository revokedTokenRepository;

    public TokenService(@Value("${app.auth.token-secret}") String configuredSecret,
                         RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
        String secret;
        if (StringUtils.hasText(configuredSecret)) {
            secret = configuredSecret;
        } else {
            secret = randomSecret();
            // Silent before this: a horizontally-scaled deployment that forgets
            // AUTH_TOKEN_SECRET gets a different secret per instance, so a token issued
            // by one instance fails validation on every other - which reads as "flaky
            // login," not the config bug it actually is. Loud on purpose, matching how
            // AdminBootstrapRunner already warns on its own missing env var.
            log.warn("app.auth.token-secret is not set - generated a random per-process secret. "
                    + "Fine for a single instance; in any multi-instance deployment, set "
                    + "AUTH_TOKEN_SECRET explicitly or sessions will fail unpredictably "
                    + "depending on which instance issued the token.");
        }
        try {
            this.mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize token signing", e);
        }
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public record Principal(Long userId, String smartIdIdentity, String displayName, Role role) {
    }

    public synchronized String issue(AppUser user) {
        long expiresAt = Instant.now().plus(TTL).getEpochSecond();
        String jti = UUID.randomUUID().toString();
        String payload = String.join("|", user.getId().toString(), user.getSmartIdIdentity(),
                user.getDisplayName(), user.getRole().name(), Long.toString(expiresAt), jti);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    public Optional<Principal> validate(String token) {
        String[] parts = decodeAndVerify(token);
        if (parts == null) {
            return Optional.empty();
        }
        long expiresAt = Long.parseLong(parts[4]);
        if (Instant.now().getEpochSecond() > expiresAt) {
            return Optional.empty();
        }
        String jti = parts[5];
        if (revokedTokenRepository.existsById(jti)) {
            return Optional.empty();
        }
        return Optional.of(new Principal(Long.valueOf(parts[0]), parts[1], parts[2], Role.valueOf(parts[3])));
    }

    /** Best-effort: an already-invalid token (bad signature, malformed, expired) has nothing meaningful to revoke, so this silently does nothing rather than erroring. */
    public void revoke(String token) {
        String[] parts = decodeAndVerify(token);
        if (parts == null) {
            return;
        }
        String jti = parts[5];
        Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(parts[4]));
        if (!revokedTokenRepository.existsById(jti)) {
            revokedTokenRepository.save(new RevokedToken(jti, Instant.now(), expiresAt));
        }
    }

    private String[] decodeAndVerify(String token) {
        int dot = token.indexOf('.');
        if (dot < 0) {
            return null;
        }
        String encodedPayload = token.substring(0, dot);
        String signature = token.substring(dot + 1);
        if (!constantTimeEquals(signature, sign(encodedPayload))) {
            return null;
        }
        String[] parts = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8)
                .split("\\|", 6);
        return parts.length == 6 ? parts : null;
    }

    private synchronized String sign(String encodedPayload) {
        byte[] signature = mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
