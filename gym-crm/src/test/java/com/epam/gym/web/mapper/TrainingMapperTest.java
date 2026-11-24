package com.epam.gym.web.mapper;

import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingType;
import com.epam.gym.web.dto.response.TraineeTrainingResponse;
import com.epam.gym.web.dto.response.TrainerTrainingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.epam.gym.TestEntities.trainee;
import static com.epam.gym.TestEntities.trainer;
import static com.epam.gym.TestEntities.training;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TrainingMapperTest {

    private final TrainingMapper trainingMapper = new TrainingMapper();

    private Training training;

    @BeforeEach
    void setUp() {
        TrainingType yoga = new TrainingType("Yoga");
        Trainee trainee = trainee("John", "Smith", "John.Smith");
        Trainer trainer = trainer("Nodira", "Karimova", "Nodira.Karimova", yoga);
        training = training(trainee, trainer, yoga, "Morning Yoga", LocalDate.of(2026, 3, 10));
    }

    @Test
    void toTraineeTrainingResponse_namesTheTrainer() {
        // given
        Training source = training;

        // when
        TraineeTrainingResponse response = trainingMapper.toTraineeTrainingResponse(source);

        // then
        assertEquals(new TraineeTrainingResponse(
                "Morning Yoga", LocalDate.of(2026, 3, 10), "Yoga", 60, "Nodira.Karimova"), response);
    }

    @Test
    void toTrainerTrainingResponse_namesTheTrainee() {
        // given
        Training source = training;

        // when
        TrainerTrainingResponse response = trainingMapper.toTrainerTrainingResponse(source);

        // then
        assertEquals(new TrainerTrainingResponse(
                "Morning Yoga", LocalDate.of(2026, 3, 10), "Yoga", 60, "John.Smith"), response);
    }
}
