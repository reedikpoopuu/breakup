package com.example.demo.datahub;

import com.example.demo.common.CountryCode;

/**
 * Read-only visibility into a DataHub adapter's configuration - never the secret
 * values themselves (client secret / API key), only whether they're set. Deliberately
 * has no write path: the market-participant identity fields are operational/regulatory
 * data meant to be set via env var at deploy time, not edited live from a browser -
 * see the discussion that led to this being a status panel instead of a form.
 */
public record DataHubStatusResponse(
        DataHubSource source,
        CountryCode country,
        boolean configured,
        String baseUrl,
        String authBaseUrl,
        String authRealm,
        String marketParticipantEic,
        String marketParticipantRole,
        boolean credentialsSet
) {
}
