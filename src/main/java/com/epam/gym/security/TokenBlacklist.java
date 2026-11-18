package com.epam.gym.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tokens revoked by logout. A token is only remembered until it would have expired anyway.
 */
@Component
public class TokenBlacklist {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenBlacklist(Clock clock) {
        this.clock = clock;
    }

    public void revoke(JwtToken token) {
        removeExpired();
        revoked.put(token.id(), token.expiresAt());
    }

    public boolean isRevoked(String tokenId) {
        Instant expiresAt = revoked.get(tokenId);
        return expiresAt != null && expiresAt.isAfter(clock.instant());
    }

    int size() {
        return revoked.size();
    }

    private void removeExpired() {
        Instant now = clock.instant();
        revoked.values().removeIf(expiresAt -> !expiresAt.isAfter(now));
    }
}
