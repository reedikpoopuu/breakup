package com.example.demo.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reconciles ADMIN status to exactly match the configured identities (Reedik Poopuu,
 * per PM_ANSWERS.txt, plus whoever else {@code app.admin.smartid-identities} lists) on
 * every boot, in both directions: creating/promoting a row for every identity in the
 * list, and demoting any {@link Role#ADMIN} row that is NOT in the list back to {@link
 * Role#USER}. Runs every startup, not just the first, so growing the admin group is
 * "add an identity to the list and restart," and - just as importantly - removing one
 * is "delete it from the list and restart," not "also go edit the database by hand."
 * Before this demotion step existed, an identity dropped from the list kept whatever
 * role it already had in the database forever, which meant an offboarded admin or a
 * compromised admin identity retained full admin API access indefinitely. {@code
 * app.admin.smartid-identities} is operational/secret data supplied via environment
 * variable at deploy time, never hard-coded (ARCH_SPEC.md section 2.2). Until it is
 * set, the app still boots (dev/test with H2), but admin login fails closed, since no
 * ADMIN row exists to match against - and any admin rows that already existed get
 * demoted too, since an unset list means "no admins," not "whatever was there before."
 */
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    // COUNTRY-NATIONALIDNUMBER, matching what SmartIdRestService.pollAuthentication()
    // resolves real logins to (identity.getCountry() + "-" + identity.getIdentityNumber()).
    // Deliberately rejects the raw Smart-ID semantics-identifier wire format
    // (e.g. PNOEE-40504040001, which SmartIdClient/SemanticsIdentifier use internally) -
    // that shape looks plausible but can never match a real login, which is exactly how
    // an ADMIN row went permanently unreachable before this class started validating.
    private static final Pattern IDENTITY_PATTERN = Pattern.compile("^[A-Z]{2}-[A-Za-z0-9]+$");

    private final AppUserRepository repository;
    private final List<String> adminSmartIdIdentities;
    private final String adminDisplayName;

    public AdminBootstrapRunner(
            AppUserRepository repository,
            @Value("${app.admin.smartid-identities}") String adminSmartIdIdentities,
            @Value("${app.admin.display-name}") String adminDisplayName) {
        this.repository = repository;
        this.adminSmartIdIdentities = parseIdentities(adminSmartIdIdentities);
        this.adminDisplayName = adminDisplayName;
    }

    private static List<String> parseIdentities(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    @Override
    public void run(String... args) {
        if (adminSmartIdIdentities.isEmpty()) {
            log.warn("app.admin.smartid-identities is not set - no ADMIN account seeded; "
                    + "admin login will fail closed until it is configured.");
        } else {
            for (String identity : adminSmartIdIdentities) {
                if (!IDENTITY_PATTERN.matcher(identity).matches()) {
                    throw new IllegalStateException(
                            "app.admin.smartid-identities contains '" + identity + "', which is not in "
                            + "COUNTRY-NATIONALIDNUMBER form (e.g. EE-40504040001). Refusing to start: "
                            + "seeding it as-is would create an ADMIN row that can never match a real "
                            + "login, silently locking every admin out.");
                }
            }
            for (String identity : adminSmartIdIdentities) {
                reconcile(identity);
            }
        }
        demoteAdminsNotInConfiguredList();
    }

    private void reconcile(String identity) {
        AppUser user = repository.findBySmartIdIdentity(identity).orElse(null);
        if (user == null) {
            repository.save(new AppUser(identity, adminDisplayName, Role.ADMIN));
            log.info("Seeded ADMIN account for {}", identity);
        } else if (user.getRole() != Role.ADMIN) {
            user.promoteToAdmin();
            repository.save(user);
            log.info("Promoted existing account {} to ADMIN", identity);
        }
    }

    private void demoteAdminsNotInConfiguredList() {
        for (AppUser user : repository.findByRole(Role.ADMIN)) {
            if (!adminSmartIdIdentities.contains(user.getSmartIdIdentity())) {
                user.demoteFromAdmin();
                repository.save(user);
                log.warn("Demoted {} from ADMIN - no longer present in app.admin.smartid-identities", user.getSmartIdIdentity());
            }
        }
    }
}
