package com.example.demo.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Seeds the initial ADMIN row (Reedik Poopuu, per PM_ANSWERS.txt) on first boot.
 * {@code app.admin.smartid-identity} is operational/secret data supplied via
 * environment variable at deploy time, never hard-coded (ARCH_SPEC.md section 2.2).
 * Until it is set, the app still boots (dev/test with H2) but admin login fails
 * closed, since no ADMIN row exists to match against.
 */
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AppUserRepository repository;
    private final String adminSmartIdIdentity;
    private final String adminDisplayName;

    public AdminBootstrapRunner(
            AppUserRepository repository,
            @Value("${app.admin.smartid-identity}") String adminSmartIdIdentity,
            @Value("${app.admin.display-name}") String adminDisplayName) {
        this.repository = repository;
        this.adminSmartIdIdentity = adminSmartIdIdentity;
        this.adminDisplayName = adminDisplayName;
    }

    @Override
    public void run(String... args) {
        if (repository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (!StringUtils.hasText(adminSmartIdIdentity)) {
            log.warn("app.admin.smartid-identity is not set - no ADMIN account seeded; "
                    + "admin login will fail closed until it is configured.");
            return;
        }
        repository.save(new AppUser(adminSmartIdIdentity, adminDisplayName, Role.ADMIN));
        log.info("Seeded initial ADMIN account for {}", adminDisplayName);
    }
}
