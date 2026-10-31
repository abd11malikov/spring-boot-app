package com.epam.gym.web.mapper;

import com.epam.gym.model.Trainee;
import com.epam.gym.model.TrainingType;
import com.epam.gym.web.dto.request.TraineeRegistrationRequest;
import com.epam.gym.web.dto.request.UpdateTraineeRequest;
import com.epam.gym.web.dto.response.RegistrationResponse;
import com.epam.gym.web.dto.response.TraineeProfileResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.epam.gym.TestEntities.trainee;
import static com.epam.gym.TestEntities.trainer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraineeMapperTest {

    private final TraineeMapper traineeMapper = new TraineeMapper(new SummaryMapper(new TrainingTypeMapper()));

    @Test
    void toEntity_registrationRequest_copiesAllFields() {
        // given
        TraineeRegistrationRequest request =
                new TraineeRegistrationRequest("John", "Doe", LocalDate.of(1998, 3, 15), "Tashkent");

        // when
        Trainee trainee = traineeMapper.toEntity(request);

        // then
        assertEquals("John", trainee.getUser().getFirstName());
        assertEquals("Doe", trainee.getUser().getLastName());
        assertEquals(LocalDate.of(1998, 3, 15), trainee.getDateOfBirth());
        assertEquals("Tashkent", trainee.getAddress());
        assertNull(trainee.getUser().getUsername());
    }

    @Test
    void toEntity_updateRequest_copiesActiveFlag() {
        // given
        UpdateTraineeRequest request = new UpdateTraineeRequest("John", "Doe", null, null, false);

        // when
        Trainee trainee = traineeMapper.toEntity(request);

        // then
        assertFalse(trainee.getUser().isActive());
        assertEquals("Doe", trainee.getUser().getLastName());
    }

    @Test
    void toRegistrationResponse_returnsGeneratedCredentials() {
        // given
        Trainee trainee = trainee("John", "Doe", "John.Doe");

        // when
        RegistrationResponse response = traineeMapper.toRegistrationResponse(trainee);

        // then
        assertEquals("John.Doe", response.username());
        assertEquals("password12", response.password());
    }

    @Test
    void toProfileResponse_includesTrainersSortedByUsername() {
        // given
        Trainee trainee = trainee("John", "Doe", "John.Doe");
        trainee.getTrainers().add(trainer("Rustam", "Aliyev", "Rustam.Aliyev", new TrainingType("Cardio")));
        trainee.getTrainers().add(trainer("Nodira", "Karimova", "Nodira.Karimova", new TrainingType("Yoga")));

        // when
        TraineeProfileResponse response = traineeMapper.toProfileResponse(trainee);

        // then
        assertEquals("John.Doe", response.username());
        assertEquals("Tashkent", response.address());
        assertTrue(response.isActive());
        assertEquals(List.of("Nodira.Karimova", "Rustam.Aliyev"),
                response.trainers().stream().map(trainer -> trainer.username()).toList());
        assertEquals("Yoga", response.trainers().get(0).specialization().trainingType());
    }
}
