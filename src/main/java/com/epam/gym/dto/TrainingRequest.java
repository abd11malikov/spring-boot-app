package com.epam.gym.dto;

import java.time.LocalDate;

public record TrainingRequest(String traineeUsername,
                              String trainerUsername,
                              String trainingName,
                              LocalDate trainingDate,
                              Integer trainingDuration) {
}
