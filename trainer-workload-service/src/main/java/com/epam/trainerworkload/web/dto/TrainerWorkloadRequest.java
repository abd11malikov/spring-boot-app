package com.epam.trainerworkload.web.dto;

import com.epam.trainerworkload.model.ActionType;
import com.epam.trainerworkload.service.TrainerWorkloadUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record TrainerWorkloadRequest(
        @Schema(example = "Nodira.Karimova") @NotBlank String trainerUsername,
        @Schema(example = "Nodira") @NotBlank String trainerFirstName,
        @Schema(example = "Karimova") @NotBlank String trainerLastName,
        @JsonProperty("isActive") @NotNull Boolean isActive,
        @Schema(example = "2026-11-10") @NotNull LocalDate trainingDate,
        @Schema(description = "Minutes", example = "60") @NotNull @Positive Integer trainingDuration,
        @NotNull ActionType actionType) {

    public TrainerWorkloadUpdate toUpdate() {
        return new TrainerWorkloadUpdate(trainerUsername, trainerFirstName, trainerLastName, isActive,
                trainingDate, trainingDuration, actionType);
    }
}
