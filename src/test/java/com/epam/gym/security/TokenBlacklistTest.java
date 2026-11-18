package com.epam.gym.security;

import com.epam.gym.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBlacklistTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    private final MutableClock clock = new MutableClock(NOW);
    private final TokenBlacklist blacklist = new TokenBlacklist(clock);

    @Test
    void revokedToken_isRevoked_othersAreNot() {
        // when
        blacklist.revoke(new JwtToken("id-1", "John.Smith", NOW.plus(Duration.ofHours(1))));

        // then
        assertTrue(blacklist.isRevoked("id-1"));
        assertFalse(blacklist.isRevoked("id-2"));
    }

    @Test
    void expiredEntries_areForgotten() {
        // given
        blacklist.revoke(new JwtToken("id-1", "John.Smith", NOW.plus(Duration.ofMinutes(10))));
        clock.advance(Duration.ofMinutes(10));

        // when
        blacklist.revoke(new JwtToken("id-2", "John.Smith", NOW.plus(Duration.ofHours(1))));

        // then
        assertFalse(blacklist.isRevoked("id-1"));
        assertEquals(1, blacklist.size());
    }
}
