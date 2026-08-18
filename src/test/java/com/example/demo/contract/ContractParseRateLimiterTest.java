package com.example.demo.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractParseRateLimiterTest {

    private final ContractParseRateLimiter rateLimiter = new ContractParseRateLimiter();

    @Test
    void allowsUpToFiveRequestsPerKeyWithinTheWindow() {
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire("EE-1")).as("request %d", i + 1).isTrue();
        }
    }

    @Test
    void rejectsTheSixthRequestForTheSameKeyWithinTheWindow() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("EE-2");
        }

        assertThat(rateLimiter.tryAcquire("EE-2")).isFalse();
    }

    @Test
    void tracksEachKeyIndependently() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire("EE-3");
        }

        assertThat(rateLimiter.tryAcquire("EE-3")).isFalse();
        assertThat(rateLimiter.tryAcquire("EE-4")).isTrue();
    }
}
