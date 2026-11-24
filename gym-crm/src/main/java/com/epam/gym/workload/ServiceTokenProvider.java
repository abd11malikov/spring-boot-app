package com.epam.gym.workload;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues the short-lived JWT Gym CRM sends to the Trainer Workload service as {@code Authorization: Bearer}.
 * <p>
 * It is signed with its own key (not the user token key), names Gym CRM as issuer and the workload service as
 * audience, so it is useless as a user token and user tokens are useless against the workload service.
 * A token is reused until shortly before it expires.
 */
public class ServiceTokenProvider {

    private static final Duration RENEW_BEFORE_EXPIRY = Duration.ofSeconds(30);

    private final SecretKey key;
    private final String issuer;
    private final String audience;
    private final Duration validity;
    private final Clock clock;
    private IssuedToken current;

    public ServiceTokenProvider(String secret, String issuer, String audience, Duration validity, Clock clock) {
        if (validity.compareTo(RENEW_BEFORE_EXPIRY.multipliedBy(2)) < 0) {
            throw new IllegalArgumentException("Service token validity must be at least " + RENEW_BEFORE_EXPIRY.multipliedBy(2));
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
        this.audience = audience;
        this.validity = validity;
        this.clock = clock;
    }

    public synchronized String getToken() {
        Instant now = clock.instant();
        if (current == null || !now.isBefore(current.expiresAt().minus(RENEW_BEFORE_EXPIRY))) {
            current = issue(now);
        }
        return current.value();
    }

    private IssuedToken issue(Instant now) {
        Instant expiresAt = now.plus(validity);
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(issuer)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    private record IssuedToken(String value, Instant expiresAt) {
    }
}
