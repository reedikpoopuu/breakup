package com.example.demo.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stands in for {@link SmartIdRestService} only when the {@code dev} Spring profile is
 * explicitly active - completes on the second poll, no network call to SK ID Solutions,
 * so local/offline development doesn't depend on sid.demo.sk.ee being reachable. Never
 * activate the {@code dev} profile outside a developer's own machine: this bean accepts
 * any identity as authenticated with no verification whatsoever. Role resolution is
 * untouched - {@link AppUserLoginService} still looks the returned identity up against
 * the real {@link AppUserRepository}, so only an identity matching the ADMIN row seeded
 * by {@link AdminBootstrapRunner} (via {@code app.admin.smartid-identity}) gets
 * ROLE_ADMIN; everyone else lands as ROLE_USER, exactly as with the real service.
 */
@Service
@Profile("dev")
public class DevSmartIdService implements SmartIdService {

    private record Session(String smartIdIdentity, String displayName, AtomicInteger pollCount) {
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger sessionCounter = new AtomicInteger();

    @Override
    public SmartIdAuthStart startAuthentication(String smartIdIdentity) {
        String sessionId = "dev-session-" + sessionCounter.incrementAndGet();
        sessions.put(sessionId, new Session(smartIdIdentity, "Dev User " + smartIdIdentity, new AtomicInteger(0)));
        return new SmartIdAuthStart(sessionId, "1234");
    }

    @Override
    public SmartIdAuthResult pollAuthentication(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return SmartIdAuthResult.failed();
        }
        if (session.pollCount().incrementAndGet() < 2) {
            return SmartIdAuthResult.running();
        }
        sessions.remove(sessionId);
        return SmartIdAuthResult.complete(session.smartIdIdentity(), session.displayName());
    }
}
