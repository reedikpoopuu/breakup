package com.example.demo.auth;

import org.springframework.stereotype.Service;

/**
 * Resolves a completed Smart-ID authentication into an {@link AppUser} row (creating one
 * as {@link Role#USER} on first login) and issues its bearer token. Shared by every
 * Smart-ID login entry point so "does this identity match an ADMIN row" stays a single
 * code path (ARCH_SPEC.md section 2.3 step 3).
 */
@Service
public class AppUserLoginService {

    public record LoginResult(String token, Role role, String displayName) {
    }

    private final AppUserRepository appUserRepository;
    private final TokenService tokenService;

    public AppUserLoginService(AppUserRepository appUserRepository, TokenService tokenService) {
        this.appUserRepository = appUserRepository;
        this.tokenService = tokenService;
    }

    public LoginResult resolveAndIssueToken(SmartIdAuthResult result) {
        AppUser user = appUserRepository.findBySmartIdIdentity(result.smartIdIdentity())
                .orElseGet(() -> appUserRepository.save(
                        new AppUser(result.smartIdIdentity(), result.displayName(), Role.USER)));
        String token = tokenService.issue(user);
        return new LoginResult(token, user.getRole(), user.getDisplayName());
    }
}
