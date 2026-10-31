package com.epam.gym.web.dto.response;

import java.time.LocalDate;

public record TraineeTrainingResponse(String trainingName,
                                      LocalDate trainingDate,
                                      String trainingType,
                                      Integer trainingDuration,
                                      String trainerName) {
}
