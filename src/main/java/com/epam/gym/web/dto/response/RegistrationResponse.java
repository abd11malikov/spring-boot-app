package com.epam.gym.web.dto.response;

public record RegistrationResponse(String username, String password) {

    @Override
    public String toString() {
        return "RegistrationResponse{username='" + username + "'}";
    }
}
