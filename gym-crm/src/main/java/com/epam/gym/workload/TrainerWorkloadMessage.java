package com.epam.gym.workload;

import com.epam.gym.model.Training;
import com.epam.gym.model.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * Body of the messages on the trainer workload queue, serialized as JSON. The Trainer Workload service reads
 * the same fields.
 *
 * @param trainingDuration minutes
 */
public record TrainerWorkloadMessage(String trainerUsername,
                                     String trainerFirstName,
                                     String trainerLastName,
                                     @JsonProperty("isActive") boolean isActive,
                                     @JsonFormat(pattern = "yyyy-MM-dd") LocalDate trainingDate,
                                     int trainingDuration,
                                     ActionType actionType) {

    public static TrainerWorkloadMessage of(Training training, ActionType actionType) {
        User trainer = training.getTrainer().getUser();
        return new TrainerWorkloadMessage(trainer.getUsername(), trainer.getFirstName(), trainer.getLastName(),
                trainer.isActive(), training.getTrainingDate(), training.getTrainingDuration(), actionType);
    }
}
