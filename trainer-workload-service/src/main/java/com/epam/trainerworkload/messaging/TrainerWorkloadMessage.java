package com.epam.trainerworkload.messaging;

import com.epam.trainerworkload.model.ActionType;
import com.epam.trainerworkload.service.TrainerWorkloadUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Body of a message on the trainer workload queue (JSON), as sent by Gym CRM.
 *
 * @param trainingDuration minutes
 */
public record TrainerWorkloadMessage(
        @NotBlank String trainerUsername,
        @NotBlank String trainerFirstName,
        @NotBlank String trainerLastName,
        @JsonProperty("isActive") @NotNull Boolean isActive,
        @NotNull LocalDate trainingDate,
        @NotNull @Positive Integer trainingDuration,
        @NotNull ActionType actionType) {

    public TrainerWorkloadUpdate toUpdate() {
        return new TrainerWorkloadUpdate(trainerUsername, trainerFirstName, trainerLastName, isActive,
                trainingDate, trainingDuration, actionType);
    }
}
