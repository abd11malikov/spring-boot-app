package com.epam.gym.web.auth;

import com.epam.gym.dto.Credentials;
import com.epam.gym.exception.AccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessGuardTest {

    private static final Credentials LOGIN = new Credentials("John.Doe", "secret");

    @Test
    void requireOwner_sameUsername_passes() {
        // given
        String username = "John.Doe";

        // when / then
        assertDoesNotThrow(() -> AccessGuard.requireOwner(LOGIN, username));
    }

    @Test
    void requireOwner_otherUsername_throwsAccessDenied() {
        // given
        String username = "Jane.Doe";

        // when
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> AccessGuard.requireOwner(LOGIN, username));

        // then
        assertEquals("You can only access your own profile", exception.getMessage());
    }
}
