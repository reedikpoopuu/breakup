package com.example.demo.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Real Smart-ID REST v2 relying-party integration (ARCH_SPEC.md section 2.3). Fails
 * closed with {@link SmartIdNotConfiguredException} until SK ID Solutions issues
 * relying-party credentials - same "credentials arrive within a few weeks" status as
 * the DataHub adapters. The actual `authentication/etsi/{semantics-identifier}` +
 * session-poll wiring against {@code app.smartid.base-url} lands once those
 * credentials are available; tests substitute {@code FakeSmartIdService} for this bean.
 */
@Service
public class SmartIdRestService implements SmartIdService {

    private final String relyingPartyUuid;
    private final String relyingPartyName;

    public SmartIdRestService(
            @Value("${app.smartid.relying-party-uuid}") String relyingPartyUuid,
            @Value("${app.smartid.relying-party-name}") String relyingPartyName) {
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
        throw new SmartIdNotConfiguredException();
    }

    @Override
    public SmartIdAuthResult pollAuthentication(String sessionId) {
        requireConfigured();
        throw new SmartIdNotConfiguredException();
    }
}
