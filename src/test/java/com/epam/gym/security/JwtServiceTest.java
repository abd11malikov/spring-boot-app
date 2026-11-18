package com.epam.gym.security;

import com.epam.gym.MutableClock;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1vbmx5LWd5bS1jcm0tand0LXNlY3JldC1rZXktMjU2LWJpdHM=";
    private static final String OTHER_SECRET = "YW5vdGhlci10ZXN0LWd5bS1jcm0tand0LXNlY3JldC1rZXktMjU2LWJpdHM=";
    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    private final MutableClock clock = new MutableClock(NOW);
    private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1), clock);

    @Test
    void generatedToken_parsesBackToUsernameAndExpiry() {
        // given
        String token = jwtService.generateToken("John.Smith");

        // when
        JwtToken parsed = jwtService.parse(token);

        // then
        assertEquals("John.Smith", parsed.username());
        assertEquals(NOW.plus(Duration.ofHours(1)), parsed.expiresAt());
    }

    @Test
    void everyToken_hasItsOwnId() {
        // when
        String first = jwtService.generateToken("John.Smith");
        String second = jwtService.generateToken("John.Smith");

        // then
        assertNotEquals(jwtService.parse(first).id(), jwtService.parse(second).id());
    }

    @Test
    void expiredToken_isRejected() {
        // given
        String token = jwtService.generateToken("John.Smith");
        clock.advance(Duration.ofHours(1).plusSeconds(1));

        // when / then
        assertThrows(ExpiredJwtException.class, () -> jwtService.parse(token));
    }

    @Test
    void tokenSignedWithOtherKey_isRejected() {
        // given
        String foreignToken = new JwtService(OTHER_SECRET, Duration.ofHours(1), clock).generateToken("John.Smith");

        // when / then
        assertThrows(JwtException.class, () -> jwtService.parse(foreignToken));
    }

    @Test
    void tamperedToken_isRejected() {
        // given
        String token = jwtService.generateToken("John.Smith");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        // when / then
        assertThrows(JwtException.class, () -> jwtService.parse(tampered));
    }

    @Test
    void tooShortSecret_isRefusedAtStartup() {
        // when / then
        assertThrows(JwtException.class, () -> new JwtService("c2hvcnQ=", Duration.ofHours(1), clock));
    }

    @Test
    void nonPositiveExpiration_isRefused() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET, Duration.ZERO, clock));
    }
}
