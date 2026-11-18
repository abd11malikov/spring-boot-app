package com.epam.gym.web.dto.response;

/**
 * @param token bearer token to use right away instead of logging in
 */
public record RegistrationResponse(String username, String password, String token) {

    @Override
    public String toString() {
        return "RegistrationResponse{username='" + username + "'}";
    }
}
