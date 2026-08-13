package com.example.demo.auth;

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

/**
 * Issues and validates signed, opaque bearer session tokens for Smart-ID logins
 * (ARCH_SPEC.md section 2.3 step 3 - "a signed session token ... with role embedded as
 * a claim"). No password/client-credentials grant exists; this is the only login path.
 */
@Service
public class TokenService {

    private static final Duration TTL = Duration.ofHours(12);

    private final Mac mac;

    public TokenService(@Value("${app.auth.token-secret}") String configuredSecret) {
        String secret = StringUtils.hasText(configuredSecret) ? configuredSecret : randomSecret();
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
        String payload = String.join("|", user.getId().toString(), user.getSmartIdIdentity(),
                user.getDisplayName(), user.getRole().name(), Long.toString(expiresAt));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    public Optional<Principal> validate(String token) {
        int dot = token.indexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }
        String encodedPayload = token.substring(0, dot);
        String signature = token.substring(dot + 1);
        if (!constantTimeEquals(signature, sign(encodedPayload))) {
            return Optional.empty();
        }
        String[] parts = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8)
                .split("\\|", 5);
        if (parts.length != 5) {
            return Optional.empty();
        }
        long expiresAt = Long.parseLong(parts[4]);
        if (Instant.now().getEpochSecond() > expiresAt) {
            return Optional.empty();
        }
        return Optional.of(new Principal(Long.valueOf(parts[0]), parts[1], parts[2], Role.valueOf(parts[3])));
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
