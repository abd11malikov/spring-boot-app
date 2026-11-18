package com.epam.gym.web.auth;

import com.epam.gym.exception.AccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessGuardTest {

    private static final String CURRENT_USER = "John.Doe";

    @Test
    void requireOwner_sameUsername_passes() {
        // given
        String username = "John.Doe";

        // when / then
        assertDoesNotThrow(() -> AccessGuard.requireOwner(CURRENT_USER, username));
    }

    @Test
    void requireOwner_otherUsername_throwsAccessDenied() {
        // given
        String username = "Jane.Doe";

        // when
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> AccessGuard.requireOwner(CURRENT_USER, username));

        // then
        assertEquals("You can only access your own profile", exception.getMessage());
    }

    @Test
    void requireOwner_noCurrentUser_throwsAccessDenied() {
        // when / then
        assertThrows(AccessDeniedException.class, () -> AccessGuard.requireOwner(null, "John.Doe"));
    }
}
