package com.epam.gym.workload;

import com.epam.gym.TestObjectMapper;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.epam.gym.TestEntities.trainee;
import static com.epam.gym.TestEntities.trainer;
import static com.epam.gym.TestEntities.training;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TrainerWorkloadMessageTest {

    @Test
    void of_takesTrainerDataFromTraining() {
        // given
        Trainee john = trainee("John", "Smith", "John.Smith");
        Trainer nodira = trainer("Nodira", "Karimova", "Nodira.Karimova", new TrainingType("Yoga"));
        nodira.getUser().setActive(false);

        // when
        TrainerWorkloadMessage request = TrainerWorkloadMessage.of(
                training(john, nodira, LocalDate.of(2026, 11, 10), 45), ActionType.DELETE);

        // then
        assertEquals(new TrainerWorkloadMessage("Nodira.Karimova", "Nodira", "Karimova", false,
                LocalDate.of(2026, 11, 10), 45, ActionType.DELETE), request);
    }

    @Test
    void json_matchesTheWorkloadServiceContract() throws Exception {
        // given
        TrainerWorkloadMessage request = new TrainerWorkloadMessage("Nodira.Karimova", "Nodira", "Karimova", true,
                LocalDate.of(2026, 11, 10), 60, ActionType.ADD);

        // when
        String json = TestObjectMapper.create().writeValueAsString(request);

        // then
        assertEquals("{\"trainerUsername\":\"Nodira.Karimova\",\"trainerFirstName\":\"Nodira\","
                + "\"trainerLastName\":\"Karimova\",\"isActive\":true,\"trainingDate\":\"2026-11-10\","
                + "\"trainingDuration\":60,\"actionType\":\"ADD\"}", json);
    }

    @Test
    void disabledClient_acceptsUpdatesWithoutCallingAnything() {
        // when / then: no exception, nothing to call
        new DisabledTrainerWorkloadPublisher().publish(new TrainerWorkloadMessage("Nodira.Karimova", "Nodira",
                "Karimova", true, LocalDate.of(2026, 11, 10), 60, ActionType.ADD));
    }
}
