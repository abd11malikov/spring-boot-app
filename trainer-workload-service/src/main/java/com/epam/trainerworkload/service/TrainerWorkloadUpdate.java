package com.epam.trainerworkload.service;

import com.epam.trainerworkload.model.ActionType;

import java.time.LocalDate;

/**
 * One training added to or deleted from a trainer's schedule.
 *
 * @param trainingDuration minutes
 */
public record TrainerWorkloadUpdate(String trainerUsername,
                                    String trainerFirstName,
                                    String trainerLastName,
                                    boolean active,
                                    LocalDate trainingDate,
                                    int trainingDuration,
                                    ActionType actionType) {
}
