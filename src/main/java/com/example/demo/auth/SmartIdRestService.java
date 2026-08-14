package com.example.demo.auth;

import ee.sk.smartid.AuthenticationHash;
import ee.sk.smartid.AuthenticationIdentity;
import ee.sk.smartid.AuthenticationRequestBuilder;
import ee.sk.smartid.AuthenticationResponseValidator;
import ee.sk.smartid.SmartIdAuthenticationResponse;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.rest.dao.Interaction;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.rest.dao.SessionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real Smart-ID REST v2 relying-party integration (ARCH_SPEC.md section 2.3), backed by
 * the official {@code ee.sk.smartid:smart-id-java-client}. {@code smartIdIdentity} is
 * always {@code COUNTRY-NATIONALIDNUMBER} (e.g. {@code EE-30303039914}), matching the key
 * {@link AdminBootstrapRunner} seeds the ADMIN row with. Still fails closed with
 * {@link SmartIdNotConfiguredException} if the relying-party UUID/name are blanked out,
 * even though they default to SK's public demo relying party. Excluded when the {@code
 * dev} profile is active, where {@link DevSmartIdService} takes over instead; tests
 * substitute {@code FakeSmartIdService} for this bean.
 */
@Service
@Profile("!dev")
public class SmartIdRestService implements SmartIdService {

    private static final Logger log = LoggerFactory.getLogger(SmartIdRestService.class);

    private record PendingSession(AuthenticationRequestBuilder request) {
    }

    private final SmartIdClient smartIdClient;
    private final String relyingPartyUuid;
    private final String relyingPartyName;
    private final Map<String, PendingSession> pendingSessions = new ConcurrentHashMap<>();

    public SmartIdRestService(
            SmartIdClient smartIdClient,
            @Value("${app.smartid.relying-party-uuid}") String relyingPartyUuid,
            @Value("${app.smartid.relying-party-name}") String relyingPartyName) {
        this.smartIdClient = smartIdClient;
        this.relyingPartyUuid = relyingPartyUuid;
        this.relyingPartyName = relyingPartyName;
    }

    private void requireConfigured() {
        if (!StringUtils.hasText(relyingPartyUuid) || !StringUtils.hasText(relyingPartyName)) {
            throw new SmartIdNotConfiguredException();
        }
    }

    @Override
    public SmartIdAuthStart startAuthentication(String smartIdIdentity) {
        requireConfigured();
        SemanticsIdentifier semanticsIdentifier = toSemanticsIdentifier(smartIdIdentity);
        AuthenticationHash hash = AuthenticationHash.generateRandomHash();
        String verificationCode = hash.calculateVerificationCode();

        AuthenticationRequestBuilder request = smartIdClient.createAuthentication()
                .withSemanticsIdentifier(semanticsIdentifier)
                .withAuthenticationHash(hash)
                // ADVANCED, not QUALIFIED: SK's public demo test accounts (e.g. the documented
                // EE-30303039914 "Testnumber, Ok" identity) return HTTP 471 "no suitable account
                // of requested type" when QUALIFIED is requested - verified against sid.demo.sk.ee.
                .withCertificateLevel("ADVANCED")
                .withAllowedInteractionsOrder(List.of(
                        Interaction.displayTextAndPIN("Log in to Baltic Energy Control Center")));

        String sessionId = request.initiateAuthentication();
        pendingSessions.put(sessionId, new PendingSession(request));
        return new SmartIdAuthStart(sessionId, verificationCode);
    }

    @Override
    public SmartIdAuthResult pollAuthentication(String sessionId) {
        PendingSession pending = pendingSessions.get(sessionId);
        if (pending == null) {
            return SmartIdAuthResult.failed();
        }

        SessionStatus sessionStatus;
        try {
            sessionStatus = smartIdClient.getSmartIdConnector().getSessionStatus(sessionId);
        } catch (RuntimeException e) {
            log.warn("Smart-ID session status lookup failed for {}", sessionId, e);
            pendingSessions.remove(sessionId);
            return SmartIdAuthResult.failed();
        }

        if (!"COMPLETE".equals(sessionStatus.getState())) {
            return SmartIdAuthResult.running();
        }
        pendingSessions.remove(sessionId);

        try {
            SmartIdAuthenticationResponse response = pending.request().createSmartIdAuthenticationResponse(sessionStatus);
            AuthenticationIdentity identity = new AuthenticationResponseValidator().validate(response);
            String resolvedIdentity = identity.getCountry() + "-" + identity.getIdentityNumber();
            String displayName = identity.getGivenName() + " " + identity.getSurname();
            return SmartIdAuthResult.complete(resolvedIdentity, displayName);
        } catch (RuntimeException e) {
            log.warn("Smart-ID authentication did not complete successfully for session {}", sessionId, e);
            return SmartIdAuthResult.failed();
        }
    }

    private static SemanticsIdentifier toSemanticsIdentifier(String smartIdIdentity) {
        String[] parts = smartIdIdentity.split("-", 2);
        if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            throw new IllegalArgumentException(
                    "Expected Smart-ID identity in COUNTRY-NATIONALIDNUMBER form, got: " + smartIdIdentity);
        }
        return new SemanticsIdentifier(SemanticsIdentifier.IdentityType.PNO, parts[0], parts[1]);
    }
}
