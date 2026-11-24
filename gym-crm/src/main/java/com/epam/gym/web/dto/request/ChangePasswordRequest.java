package com.epam.gym.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Schema(example = "John.Doe") @NotBlank String username,
        @NotBlank String oldPassword,
        @NotBlank @Size(max = 255) String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest{username='" + username + "'}";
    }
}
