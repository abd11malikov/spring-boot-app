package com.epam.gym.web.auth;

import com.epam.gym.dto.Credentials;
import com.epam.gym.exception.AccessDeniedException;

public final class AccessGuard {

    private AccessGuard() {
    }

    public static void requireOwner(Credentials credentials, String username) {
        if (!credentials.username().equals(username)) {
            throw new AccessDeniedException("You can only access your own profile");
        }
    }
}
