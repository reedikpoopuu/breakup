package com.example.demo.auth;

import ee.sk.smartid.AuthenticationCertificateLevel;
import ee.sk.smartid.AuthenticationIdentity;
import ee.sk.smartid.CertificateValidator;
import ee.sk.smartid.CertificateValidatorImpl;
import ee.sk.smartid.FileTrustedCAStoreBuilder;
import ee.sk.smartid.HashAlgorithm;
import ee.sk.smartid.NotificationAuthenticationResponseValidator;
import ee.sk.smartid.RpChallenge;
import ee.sk.smartid.RpChallengeGenerator;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.VerificationCodeCalculator;
import ee.sk.smartid.common.notification.interactions.NotificationInteraction;
import ee.sk.smartid.rest.dao.NotificationAuthenticationSessionRequest;
import ee.sk.smartid.rest.dao.NotificationAuthenticationSessionResponse;
import ee.sk.smartid.rest.dao.SemanticsIdentifier;
import ee.sk.smartid.rest.dao.SessionStatus;
import ee.sk.smartid.signature.AuthenticationSignatureAlgorithm;
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
 * Real Smart-ID REST v3 relying-party integration (ARCH_SPEC.md section 2.3), backed by
 * the official {@code ee.sk.smartid:smart-id-java-client}, using the notification-based
 * authentication flow (push notification + verification code, no QR code/device link).
 * {@code smartIdIdentity} is always {@code COUNTRY-NATIONALIDNUMBER} (e.g. {@code
 * EE-40504040001}, the RP API v3 notification-flow demo account per
 * https://sk-eid.github.io/smart-id-documentation/rp-api/test_accounts.html - the older
 * {@code EE-30303039914} demo identity from the v2/device-link docs does not work here),
 * matching the key {@link AdminBootstrapRunner} seeds the ADMIN row with. Still fails
 * closed with {@link SmartIdNotConfiguredException} if the
 * relying-party UUID/name are blanked out, even though they default to SK's public demo
 * relying party. Excluded when the {@code dev} profile is active, where {@link
 * DevSmartIdService} takes over instead; tests substitute {@code FakeSmartIdService} for
 * this bean.
 */
@Service
@Profile("!dev")
public class SmartIdRestService implements SmartIdService {

    private static final Logger log = LoggerFactory.getLogger(SmartIdRestService.class);

    private record PendingSession(NotificationAuthenticationSessionRequest request) {
    }

    private final SmartIdClient smartIdClient;
    private final String relyingPartyUuid;
    private final String relyingPartyName;
    private final String schemeName;
    private final NotificationAuthenticationResponseValidator authenticationResponseValidator;
    private final Map<String, PendingSession> pendingSessions = new ConcurrentHashMap<>();

    public SmartIdRestService(
            SmartIdClient smartIdClient,
            @Value("${app.smartid.relying-party-uuid}") String relyingPartyUuid,
            @Value("${app.smartid.relying-party-name}") String relyingPartyName,
            @Value("${app.smartid.scheme-name}") String schemeName) {
        this.smartIdClient = smartIdClient;
        this.relyingPartyUuid = relyingPartyUuid;
        this.relyingPartyName = relyingPartyName;
        this.schemeName = schemeName;
        // FileTrustedCAStoreBuilder loads two classpath resources by default, and the published
        // smart-id-java-client 3.2 jar ships both incomplete (verified against the actual jar
        // from Maven Central):
        //  - /sid_trust_anchor_certificates.jks (trust anchors / root CAs) isn't bundled at all -
        //    build() fails with "trustAnchors parameter must be non-empty" without it.
        //  - /trusted_certificates.jks (intermediate CAs) only has the 2 oldest production
        //    intermediates; it's missing both the newer production ones (EID_NQ_2021E/R,
        //    EID_Q_2024E/R - shipped as loose .der.crt files in the jar but never wired into the
        //    default jks) and the DEMO/TEST-environment ones entirely, so no DEMO session -
        //    including the mock accounts below - can validate out of the box.
        // Both files under src/main/resources/ here replace those defaults with fuller stores
        // assembled from SK's own published material (the library's test-suite trust-anchor
        // store, plus every intermediate cert - production and demo/test - found anywhere in the
        // 3.2 jar/repo), placed at the same default classpath paths to close the gap.
        CertificateValidator certificateValidator = new CertificateValidatorImpl(new FileTrustedCAStoreBuilder().build());
        this.authenticationResponseValidator = NotificationAuthenticationResponseValidator.defaultSetupWithCertificateValidator(certificateValidator);
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
        RpChallenge rpChallenge = RpChallengeGenerator.generate();
        String verificationCode = VerificationCodeCalculator.calculate(rpChallenge.value());

        var builder = smartIdClient.createNotificationAuthentication()
                .withSemanticsIdentifier(semanticsIdentifier)
                .withRpChallenge(rpChallenge.toBase64EncodedValue())
                // QUALIFIED, not ADVANCED: SK's public demo relying party (per
                // https://sk-eid.github.io/smart-id-documentation/environments.html#_demo)
                // has no access to Smart-ID Basic (ADVANCED) accounts - requesting
                // ADVANCED against it returns HTTP 403 Forbidden.
                .withCertificateLevel(AuthenticationCertificateLevel.QUALIFIED)
                .withSignatureAlgorithm(AuthenticationSignatureAlgorithm.RSASSA_PSS)
                .withHashAlgorithm(HashAlgorithm.SHA3_512)
                .withInteractions(List.of(
                        NotificationInteraction.displayTextAndPin("Log in to Baltic Energy Control Center")));

        NotificationAuthenticationSessionResponse response = builder.initAuthenticationSession();
        pendingSessions.put(response.sessionID(), new PendingSession(builder.getAuthenticationSessionRequest()));
        return new SmartIdAuthStart(response.sessionID(), verificationCode);
    }

    @Override
    public SmartIdAuthResult pollAuthentication(String sessionId) {
        PendingSession pending = pendingSessions.get(sessionId);
        if (pending == null) {
            return SmartIdAuthResult.failed();
        }

        SessionStatus sessionStatus;
        try {
            sessionStatus = smartIdClient.getSessionStatusPoller().getSessionStatus(sessionId);
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
            AuthenticationIdentity identity = authenticationResponseValidator.validate(sessionStatus, pending.request(), schemeName);
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
