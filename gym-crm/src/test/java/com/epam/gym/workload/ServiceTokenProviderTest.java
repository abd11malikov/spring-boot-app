package com.epam.gym.workload;

import com.epam.gym.MutableClock;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTokenProviderTest {

    private static final String SECRET = "ZGV2LW9ubHktZ3ltLXNlcnZpY2UtdG8tc2VydmljZS1qd3Qta2V5LTI1Ng==";
    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    private final MutableClock clock = new MutableClock(NOW);
    private final ServiceTokenProvider provider =
            new ServiceTokenProvider(SECRET, "gym-crm", "trainer-workload-service", Duration.ofMinutes(5), clock);

    @Test
    void token_namesIssuerAudienceAndExpiry() {
        // when
        Claims claims = parse(provider.getToken());

        // then
        assertEquals("gym-crm", claims.getIssuer());
        assertEquals("gym-crm", claims.getSubject());
        assertTrue(claims.getAudience().contains("trainer-workload-service"));
        assertEquals(NOW.plus(Duration.ofMinutes(5)), claims.getExpiration().toInstant());
    }

    @Test
    void token_isReusedWhileFarFromExpiry() {
        // given
        String first = provider.getToken();

        // when
        clock.advance(Duration.ofMinutes(4));

        // then
        assertEquals(first, provider.getToken());
    }

    @Test
    void token_isRenewedShortlyBeforeExpiry() {
        // given
        String first = provider.getToken();

        // when
        clock.advance(Duration.ofMinutes(4).plusSeconds(31));

        // then
        assertNotEquals(first, provider.getToken());
    }

    @Test
    void tooShortValidity_isRefused() {
        // when / then
        assertThrows(IllegalArgumentException.class, () ->
                new ServiceTokenProvider(SECRET, "gym-crm", "trainer-workload-service", Duration.ofSeconds(30), clock));
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .clock(() -> java.util.Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
