package com.example.demo.auth.support;

import com.example.demo.auth.SmartIdAuthResult;
import com.example.demo.auth.SmartIdAuthStart;
import com.example.demo.auth.SmartIdService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Stands in for {@code SmartIdRestService} in tests - completes on the second poll, no real network call. */
public class FakeSmartIdService implements SmartIdService {

    private record Session(String smartIdIdentity, String displayName, AtomicInteger pollCount) {
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger sessionCounter = new AtomicInteger();

    @Override
    public SmartIdAuthStart startAuthentication(String smartIdIdentity) {
        String sessionId = "session-" + sessionCounter.incrementAndGet();
        sessions.put(sessionId, new Session(smartIdIdentity, "Test User " + smartIdIdentity, new AtomicInteger(0)));
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
        return SmartIdAuthResult.complete(session.smartIdIdentity(), session.displayName());
    }
}
