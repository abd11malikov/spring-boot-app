package com.epam.gym.web.auth;

import com.epam.gym.dto.Credentials;
import com.epam.gym.exception.AuthenticationFailedException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredentialsArgumentResolverTest {

    private static final String INVALID_HEADER = "Missing or invalid Authorization header";

    private final CredentialsArgumentResolver resolver = new CredentialsArgumentResolver();

    @Test
    void parse_validBasicHeader_returnsCredentials() {
        // given
        String header = "Basic " + encode("John.Doe:secret");

        // when
        Credentials credentials = resolver.parse(header);

        // then
        assertEquals(new Credentials("John.Doe", "secret"), credentials);
    }

    @Test
    void parse_passwordContainingColon_keepsWholePassword() {
        // given
        String header = "Basic " + encode("John.Doe:se:cr:et");

        // when
        Credentials credentials = resolver.parse(header);

        // then
        assertEquals("se:cr:et", credentials.password());
    }

    @Test
    void parse_lowerCaseScheme_isAccepted() {
        // given
        String header = "basic " + encode("John.Doe:secret");

        // when
        Credentials credentials = resolver.parse(header);

        // then
        assertEquals("John.Doe", credentials.username());
    }

    @Test
    void parse_missingHeader_throwsException() {
        // given
        String header = null;

        // when
        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class,
                () -> resolver.parse(header));

        // then
        assertEquals(INVALID_HEADER, exception.getMessage());
    }

    @Test
    void parse_bearerScheme_throwsException() {
        // given
        String header = "Bearer some-token";

        // when
        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class,
                () -> resolver.parse(header));

        // then
        assertEquals(INVALID_HEADER, exception.getMessage());
    }

    @Test
    void parse_notBase64_throwsException() {
        // given
        String header = "Basic ***not-base64***";

        // when
        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class,
                () -> resolver.parse(header));

        // then
        assertEquals(INVALID_HEADER, exception.getMessage());
    }

    @Test
    void parse_noColonSeparator_throwsException() {
        // given
        String header = "Basic " + encode("JohnDoeWithoutPassword");

        // when
        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class,
                () -> resolver.parse(header));

        // then
        assertEquals(INVALID_HEADER, exception.getMessage());
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
