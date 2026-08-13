package com.example.demo.auth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Smart-ID authentication endpoints - the only login path, for both regular users and
 * the ADMIN role (ARCH_SPEC.md section 2.3). Contract-signing reuses the identical
 * start/poll shape against Smart-ID's signature endpoint, added separately.
 */
@RestController
@RequestMapping("/auth/smartid")
public class SmartIdController {

    public record StartRequest(@NotBlank String smartIdIdentity) {
    }

    private final SmartIdService smartIdService;
    private final AppUserRepository appUserRepository;
    private final TokenService tokenService;

    public SmartIdController(SmartIdService smartIdService, AppUserRepository appUserRepository,
                              TokenService tokenService) {
        this.smartIdService = smartIdService;
        this.appUserRepository = appUserRepository;
        this.tokenService = tokenService;
    }

    @PostMapping("/start")
    public SmartIdAuthStart start(@RequestBody StartRequest request) {
        return smartIdService.startAuthentication(request.smartIdIdentity());
    }

    @GetMapping("/poll")
    public SmartIdPollResponse poll(@RequestParam String sessionId) {
        SmartIdAuthResult result = smartIdService.pollAuthentication(sessionId);
        if (result.status() != SmartIdAuthStatus.COMPLETE) {
            return SmartIdPollResponse.pending(result.status());
        }
        AppUser user = appUserRepository.findBySmartIdIdentity(result.smartIdIdentity())
                .orElseGet(() -> appUserRepository.save(
                        new AppUser(result.smartIdIdentity(), result.displayName(), Role.USER)));
        String token = tokenService.issue(user);
        return SmartIdPollResponse.issued(token, user.getRole(), user.getDisplayName());
    }
}
