package com.epam.gym.security;

import java.time.Instant;

/**
 * The validated content of a bearer token.
 *
 * @param id        unique token id (the {@code jti} claim), used to revoke the token on logout
 * @param username  the authenticated user (the {@code sub} claim)
 * @param expiresAt when the token stops being valid
 */
public record JwtToken(String id, String username, Instant expiresAt) {
}
