package com.example.demo.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TokenServiceTest {

    @Autowired
    TokenService tokenService;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    RevokedTokenRepository revokedTokenRepository;

    @Test
    void freshlyIssuedTokenValidates() {
        AppUser user = appUserRepository.save(new AppUser("EE-token-" + System.nanoTime(), "Test User", Role.USER));

        String token = tokenService.issue(user);

        assertThat(tokenService.validate(token)).isPresent();
        assertThat(tokenService.validate(token).get().smartIdIdentity()).isEqualTo(user.getSmartIdIdentity());
    }

    @Test
    void revokedTokenNoLongerValidates() {
        AppUser user = appUserRepository.save(new AppUser("EE-token-" + System.nanoTime(), "Test User", Role.USER));
        String token = tokenService.issue(user);
        assertThat(tokenService.validate(token)).isPresent();

        tokenService.revoke(token);

        assertThat(tokenService.validate(token)).isEmpty();
    }

    @Test
    void revokingOneTokenDoesNotAffectAnotherTokenForTheSameUser() {
        AppUser user = appUserRepository.save(new AppUser("EE-token-" + System.nanoTime(), "Test User", Role.USER));
        String firstToken = tokenService.issue(user);
        String secondToken = tokenService.issue(user);

        tokenService.revoke(firstToken);

        assertThat(tokenService.validate(firstToken)).isEmpty();
        assertThat(tokenService.validate(secondToken)).isPresent();
    }

    @Test
    void revokingAMalformedTokenIsANoOpRatherThanThrowing() {
        long before = revokedTokenRepository.count();

        tokenService.revoke("not-a-real-token");
        tokenService.revoke("");

        assertThat(revokedTokenRepository.count()).isEqualTo(before);
    }

    @Test
    void revokingTheSameTokenTwiceIsHarmless() {
        AppUser user = appUserRepository.save(new AppUser("EE-token-" + System.nanoTime(), "Test User", Role.USER));
        String token = tokenService.issue(user);

        tokenService.revoke(token);
        tokenService.revoke(token);

        assertThat(tokenService.validate(token)).isEmpty();
    }
}
