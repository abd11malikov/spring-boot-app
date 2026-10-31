package com.epam.gym.web.mapper;

import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingType;
import com.epam.gym.web.dto.request.TrainerRegistrationRequest;
import com.epam.gym.web.dto.request.UpdateTrainerRequest;
import com.epam.gym.web.dto.response.RegistrationResponse;
import com.epam.gym.web.dto.response.TrainerProfileResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.epam.gym.TestEntities.trainee;
import static com.epam.gym.TestEntities.trainer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class TrainerMapperTest {

    private final TrainingTypeMapper trainingTypeMapper = new TrainingTypeMapper();
    private final TrainerMapper trainerMapper =
            new TrainerMapper(new SummaryMapper(trainingTypeMapper), trainingTypeMapper);

    @Test
    void toEntity_registrationRequest_copiesNamesOnly() {
        // given
        TrainerRegistrationRequest request = new TrainerRegistrationRequest("Nodira", "Karimova", 2L);

        // when
        Trainer trainer = trainerMapper.toEntity(request);

        // then
        assertEquals("Nodira", trainer.getUser().getFirstName());
        assertEquals("Karimova", trainer.getUser().getLastName());
        assertNull(trainer.getSpecialization());
    }

    @Test
    void toEntity_updateRequest_copiesActiveFlag() {
        // given
        UpdateTrainerRequest request = new UpdateTrainerRequest("Nodira", "Aliyeva", false);

        // when
        Trainer trainer = trainerMapper.toEntity(request);

        // then
        assertEquals("Aliyeva", trainer.getUser().getLastName());
        assertFalse(trainer.getUser().isActive());
    }

    @Test
    void toRegistrationResponse_returnsGeneratedCredentials() {
        // given
        Trainer trainer = trainer("Nodira", "Karimova", "Nodira.Karimova", new TrainingType("Yoga"));

        // when
        RegistrationResponse response = trainerMapper.toRegistrationResponse(trainer);

        // then
        assertEquals("Nodira.Karimova", response.username());
        assertEquals("password12", response.password());
    }

    @Test
    void toProfileResponse_includesSpecializationAndSortedTrainees() {
        // given
        Trainer trainer = trainer("Nodira", "Karimova", "Nodira.Karimova", new TrainingType("Yoga"));
        trainer.getTrainees().add(trainee("John", "Smith", "John.Smith"));
        trainer.getTrainees().add(trainee("Ali", "Valiyev", "Ali.Valiyev"));

        // when
        TrainerProfileResponse response = trainerMapper.toProfileResponse(trainer);

        // then
        assertEquals("Yoga", response.specialization().trainingType());
        assertEquals(List.of("Ali.Valiyev", "John.Smith"),
                response.trainees().stream().map(trainee -> trainee.username()).toList());
    }
}
