package com.epam.gym.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates the HMAC-SHA signed JWT bearer tokens returned by registration and login.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;
    private final JwtParser parser;

    public JwtService(@Value("${gym.security.jwt.secret}") String secret,
                      @Value("${gym.security.jwt.expiration}") Duration expiration,
                      Clock clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be empty");
        }
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiration = expiration;
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    public String generateToken(String username) {
        Instant now = clock.instant();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * @throws JwtException if the token is malformed, has a wrong signature or is expired
     */
    public JwtToken parse(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        if (claims.getSubject() == null || claims.getId() == null || claims.getExpiration() == null) {
            throw new JwtException("Token is missing required claims");
        }
        return new JwtToken(claims.getId(), claims.getSubject(), claims.getExpiration().toInstant());
    }

    public Duration getExpiration() {
        return expiration;
    }
}
