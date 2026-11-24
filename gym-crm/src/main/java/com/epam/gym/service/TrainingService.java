package com.epam.gym.service;

import com.epam.gym.dto.TrainingRequest;
import com.epam.gym.model.Training;

import java.time.LocalDate;
import java.util.List;

/**
 * Training operations on behalf of the already authenticated user {@code username}.
 */
public interface TrainingService {
    Training addTraining(String username, TrainingRequest request);

    List<Training> getTraineeTrainings(String username, LocalDate fromDate, LocalDate toDate,
                                       String trainerUsername, String trainingTypeName);

    List<Training> getTrainerTrainings(String username, LocalDate fromDate, LocalDate toDate,
                                       String traineeUsername);
}
