package com.example.demo.registry;

import com.example.demo.common.CountryCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * The actual "is the logged-in user's personal ID visible in the company's
 * representative-people list" check (ARCHITECTURE.md section 3.2). Fetches every
 * person the business registry lists as holding a right of representation over the
 * company, then looks for one whose personal ID code + country matches the
 * authenticated Smart-ID identity - not just the code alone, since a personal ID
 * number is only unique within its issuing country.
 */
@Service
public class RepresentativeRightsVerificationService {

    // Smart-ID identities carry ISO 3166-1 alpha-2 (see SmartIdRestService /
    // TokenService.Principal); the business registry reports alpha-3 (e.g. "EST") -
    // this app only ever authenticates users from the three countries it operates in.
    private static final Map<String, String> ALPHA2_TO_ALPHA3 = Map.of(
            "EE", "EST",
            "LV", "LVA",
            "LT", "LTU"
    );

    private final RepresentationRightsClientResolver resolver;

    public RepresentativeRightsVerificationService(RepresentationRightsClientResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * @param country      the company's country - selects which national registry adapter to call
     * @param registryCode the company's national business-registry code
     * @param smartIdIdentity the authenticated user's login identity, {@code
     *                        COUNTRY-NATIONALIDNUMBER} (see {@code
     *                        TokenService.Principal.smartIdIdentity()})
     */
    public RepresentationRightsVerification verify(CountryCode country, String registryCode, String smartIdIdentity) {
        String[] parts = smartIdIdentity.split("-", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Expected Smart-ID identity in COUNTRY-NATIONALIDNUMBER form, got: " + smartIdIdentity);
        }
        String personalIdCode = parts[1];
        String personalIdCountry = ALPHA2_TO_ALPHA3.getOrDefault(parts[0], parts[0]);

        RepresentationRightsClient client = resolver.resolve(country);
        List<CompanyRepresentative> representatives = client.fetchRepresentatives(registryCode);

        return representatives.stream()
                .filter(r -> personalIdCode.equals(r.personalIdCode())
                        && personalIdCountry.equalsIgnoreCase(r.personalIdCountry()))
                .findFirst()
                .map(match -> RepresentationRightsVerification.verified(registryCode, match))
                .orElseGet(() -> RepresentationRightsVerification.notVerified(registryCode, personalIdCode, personalIdCountry));
    }
}
