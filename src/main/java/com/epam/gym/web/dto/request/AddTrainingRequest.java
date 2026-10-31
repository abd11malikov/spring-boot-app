package com.epam.gym.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AddTrainingRequest(
        @Schema(example = "John.Doe") @NotBlank String traineeUsername,
        @Schema(example = "Nodira.Karimova") @NotBlank String trainerUsername,
        @Schema(example = "Morning Yoga") @NotBlank @Size(max = 255) String trainingName,
        @Schema(example = "2026-11-10") @NotNull LocalDate trainingDate,
        @Schema(description = "Duration in minutes", example = "60") @NotNull @Positive Integer trainingDuration) {
}
