package com.epam.gym.security;

import com.epam.gym.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-18T10:00:00Z"));
    private final LoginAttemptService service = new LoginAttemptService(3, Duration.ofMinutes(5), clock);

    @Test
    void twoFailures_doNotBlock() {
        // when
        service.loginFailed("John.Smith");
        service.loginFailed("John.Smith");

        // then
        assertFalse(service.isBlocked("John.Smith"));
    }

    @Test
    void thirdFailure_blocksTheUserOnly() {
        // when
        failThreeTimes("John.Smith");

        // then
        assertTrue(service.isBlocked("John.Smith"));
        assertFalse(service.isBlocked("Jane.Doe"));
    }

    @Test
    void block_liftsAfterFiveMinutes() {
        // given
        failThreeTimes("John.Smith");

        // when
        clock.advance(Duration.ofMinutes(5).minusSeconds(1));
        boolean blockedJustBefore = service.isBlocked("John.Smith");
        clock.advance(Duration.ofSeconds(1));

        // then
        assertTrue(blockedJustBefore);
        assertFalse(service.isBlocked("John.Smith"));
    }

    @Test
    void afterBlockExpires_counterStartsFromZero() {
        // given
        failThreeTimes("John.Smith");
        clock.advance(Duration.ofMinutes(5));

        // when
        service.loginFailed("John.Smith");
        service.loginFailed("John.Smith");

        // then
        assertFalse(service.isBlocked("John.Smith"));
    }

    @Test
    void failuresWhileBlocked_doNotExtendTheBlock() {
        // given
        failThreeTimes("John.Smith");
        clock.advance(Duration.ofMinutes(4));

        // when
        service.loginFailed("John.Smith");
        clock.advance(Duration.ofMinutes(1));

        // then
        assertFalse(service.isBlocked("John.Smith"));
    }

    @Test
    void success_resetsFailures() {
        // given
        service.loginFailed("John.Smith");
        service.loginFailed("John.Smith");

        // when
        service.loginSucceeded("John.Smith");
        service.loginFailed("John.Smith");

        // then
        assertFalse(service.isBlocked("John.Smith"));
    }

    @Test
    void invalidSettings_areRefused() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new LoginAttemptService(0, Duration.ofMinutes(5), clock));
        assertThrows(IllegalArgumentException.class, () -> new LoginAttemptService(3, Duration.ZERO, clock));
    }

    private void failThreeTimes(String username) {
        for (int i = 0; i < 3; i++) {
            service.loginFailed(username);
        }
    }
}
