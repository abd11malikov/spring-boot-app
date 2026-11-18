package com.epam.gym.web.auth;

import com.epam.gym.exception.AccessDeniedException;

public final class AccessGuard {

    private AccessGuard() {
    }

    /**
     * @param currentUser username of the authenticated caller
     * @param username    owner of the requested resource
     */
    public static void requireOwner(String currentUser, String username) {
        if (currentUser == null || !currentUser.equals(username)) {
            throw new AccessDeniedException("You can only access your own profile");
        }
    }
}
