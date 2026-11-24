package com.epam.gym.workload;

import com.epam.gym.model.Training;
import com.epam.gym.model.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * The contract of {@code POST /api/trainer-workloads} of the Trainer Workload service.
 *
 * @param trainingDuration minutes
 */
public record TrainerWorkloadRequest(String trainerUsername,
                                     String trainerFirstName,
                                     String trainerLastName,
                                     @JsonProperty("isActive") boolean isActive,
                                     @JsonFormat(pattern = "yyyy-MM-dd") LocalDate trainingDate,
                                     int trainingDuration,
                                     ActionType actionType) {

    public static TrainerWorkloadRequest of(Training training, ActionType actionType) {
        User trainer = training.getTrainer().getUser();
        return new TrainerWorkloadRequest(trainer.getUsername(), trainer.getFirstName(), trainer.getLastName(),
                trainer.isActive(), training.getTrainingDate(), training.getTrainingDuration(), actionType);
    }
}
