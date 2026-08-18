package com.example.demo.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Falls under SecurityConfig's {@code anyRequest().authenticated()} rule (not {@code
 * permitAll()}), so by the time this method runs, BearerTokenAuthFilter has already
 * proven the token's signature valid and not yet revoked - only a currently-valid
 * session can revoke itself, which is exactly the semantic wanted.
 */
@RestController
@RequestMapping("/api/auth")
public class LogoutController {

    private final TokenService tokenService;

    public LogoutController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader.startsWith("Bearer ")) {
            tokenService.revoke(authorizationHeader.substring("Bearer ".length()));
        }
    }
}
