package com.epam.gym.web.dto.response;

/**
 * @param token     JWT to send as {@code Authorization: Bearer <token>}
 * @param expiresIn token lifetime in seconds
 */
public record TokenResponse(String token, String tokenType, long expiresIn) {

    public static TokenResponse bearer(String token, long expiresIn) {
        return new TokenResponse(token, "Bearer", expiresIn);
    }

    @Override
    public String toString() {
        return "TokenResponse{tokenType='" + tokenType + "', expiresIn=" + expiresIn + "}";
    }
}
