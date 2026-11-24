package com.epam.gym.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTraineeRequest(
        @Schema(example = "John") @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "Doe") @NotBlank @Size(max = 100) String lastName,
        @Schema(example = "1998-03-15") @Past LocalDate dateOfBirth,
        @Schema(example = "15 Navoi Street, Tashkent") @Size(max = 255) String address,
        @Schema(example = "true") @NotNull Boolean isActive) {
}
