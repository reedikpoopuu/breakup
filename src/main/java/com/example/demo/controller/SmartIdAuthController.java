package com.example.demo.controller;

import com.example.demo.auth.AppUserLoginService;
import com.example.demo.auth.SmartIdAuthResult;
import com.example.demo.auth.SmartIdAuthStart;
import com.example.demo.auth.SmartIdAuthStatus;
import com.example.demo.auth.SmartIdPollResponse;
import com.example.demo.auth.SmartIdService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Smart-ID login, split into country code + national identity number fields. Fronts
 * {@link SmartIdService} / {@link AppUserLoginService} - an identity only reaches
 * ROLE_ADMIN if it matches the ADMIN row seeded by {@code AdminBootstrapRunner}.
 */
@RestController
@RequestMapping("/api/auth/smart-id")
public class SmartIdAuthController {

    public record StartRequest(@NotBlank String countryCode, @NotBlank String nationalIdentityNumber) {
    }

    private final SmartIdService smartIdService;
    private final AppUserLoginService loginService;

    public SmartIdAuthController(SmartIdService smartIdService, AppUserLoginService loginService) {
        this.smartIdService = smartIdService;
        this.loginService = loginService;
    }

    @PostMapping("/start")
    public SmartIdAuthStart start(@RequestBody @Valid StartRequest request) {
        String smartIdIdentity = request.countryCode().trim().toUpperCase() + "-" + request.nationalIdentityNumber().trim();
        return smartIdService.startAuthentication(smartIdIdentity);
    }

    @GetMapping("/poll/{sessionId}")
    public SmartIdPollResponse poll(@PathVariable String sessionId) {
        SmartIdAuthResult result = smartIdService.pollAuthentication(sessionId);
        if (result.status() != SmartIdAuthStatus.COMPLETE) {
            return SmartIdPollResponse.pending(result.status());
        }
        AppUserLoginService.LoginResult login = loginService.resolveAndIssueToken(result);
        return SmartIdPollResponse.issued(login.token(), login.role(), login.displayName());
    }
}
