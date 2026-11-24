package com.epam.gym.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute force protection: after {@code maxAttempts} unsuccessful logins in a row a username is
 * blocked for {@code blockDuration}, even for the right password.
 */
@Service
public class LoginAttemptService {
    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final int maxAttempts;
    private final Duration blockDuration;
    private final Clock clock;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${gym.security.brute-force.max-attempts}") int maxAttempts,
                               @Value("${gym.security.brute-force.block-duration}") Duration blockDuration,
                               Clock clock) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Max login attempts must be positive");
        }
        if (blockDuration == null || blockDuration.isNegative() || blockDuration.isZero()) {
            throw new IllegalArgumentException("Block duration must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.blockDuration = blockDuration;
        this.clock = clock;
    }

    public void loginFailed(String username) {
        Instant now = clock.instant();
        Attempts updated = attempts.compute(username, (key, current) -> {
            if (current == null || current.isBlockExpired(now)) {
                current = new Attempts(0, null);
            }
            if (current.isBlocked(now)) {
                return current;
            }
            int failures = current.failures() + 1;
            return failures >= maxAttempts
                    ? new Attempts(0, now.plus(blockDuration))
                    : new Attempts(failures, null);
        });
        if (updated.isBlocked(now)) {
            log.warn("User {} is blocked until {} after {} failed logins", username, updated.blockedUntil(), maxAttempts);
        }
    }

    public void loginSucceeded(String username) {
        attempts.remove(username);
    }

    public boolean isBlocked(String username) {
        Attempts current = attempts.get(username);
        if (current == null) {
            return false;
        }
        Instant now = clock.instant();
        if (current.isBlockExpired(now)) {
            attempts.remove(username, current);
            return false;
        }
        return current.isBlocked(now);
    }

    public Duration getBlockDuration() {
        return blockDuration;
    }

    private record Attempts(int failures, Instant blockedUntil) {

        boolean isBlocked(Instant now) {
            return blockedUntil != null && blockedUntil.isAfter(now);
        }

        boolean isBlockExpired(Instant now) {
            return blockedUntil != null && !blockedUntil.isAfter(now);
        }
    }
}
