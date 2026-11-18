package com.epam.gym.service;

public interface AuthenticationService {

    /**
     * Replaces the password of {@code username} after checking its current password.
     */
    void changePassword(String username, String oldPassword, String newPassword);
}
