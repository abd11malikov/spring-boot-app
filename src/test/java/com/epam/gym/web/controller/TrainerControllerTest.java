package com.epam.gym.web.controller;

import com.epam.gym.dto.Credentials;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingType;
import com.epam.gym.service.TrainerService;
import com.epam.gym.service.TrainingService;
import com.epam.gym.web.dto.request.ActivationRequest;
import com.epam.gym.web.dto.request.TrainerRegistrationRequest;
import com.epam.gym.web.dto.request.UpdateTrainerRequest;
import com.epam.gym.web.mapper.SummaryMapper;
import com.epam.gym.web.mapper.TrainerMapper;
import com.epam.gym.web.mapper.TrainingMapper;
import com.epam.gym.web.mapper.TrainingTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static com.epam.gym.TestEntities.trainee;
import static com.epam.gym.TestEntities.trainer;
import static com.epam.gym.TestEntities.training;
import static com.epam.gym.web.controller.ControllerTestSupport.basicAuth;
import static com.epam.gym.web.controller.ControllerTestSupport.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainerControllerTest {

    private static final Credentials NODIRA_LOGIN = new Credentials("Nodira.Karimova", "password12");
    private static final String NODIRA_AUTH = basicAuth("Nodira.Karimova", "password12");

    @Mock
    private TrainerService trainerService;

    @Mock
    private TrainingService trainingService;

    private MockMvc mockMvc;

    private Trainer nodira;

    @BeforeEach
    void setUp() {
        TrainingTypeMapper trainingTypeMapper = new TrainingTypeMapper();
        TrainerController controller = new TrainerController(trainerService, trainingService,
                new TrainerMapper(new SummaryMapper(trainingTypeMapper), trainingTypeMapper), new TrainingMapper());
        mockMvc = ControllerTestSupport.mockMvc(controller);

        nodira = trainer("Nodira", "Karimova", "Nodira.Karimova", new TrainingType("Yoga"));
    }

    @Test
    void register_validRequest_passesSpecializationIdAndReturns201() throws Exception {
        // given
        TrainerRegistrationRequest request = new TrainerRegistrationRequest("Nodira", "Karimova", 2L);
        when(trainerService.create(any(Trainer.class), eq(2L))).thenReturn(nodira);

        // when
        ResultActions result = mockMvc.perform(post("/api/trainers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Nodira.Karimova"))
                .andExpect(jsonPath("$.password").value("password12"));
    }

    @Test
    void register_missingSpecialization_returns400() throws Exception {
        // given
        String body = "{\"firstName\":\"Nodira\",\"lastName\":\"Karimova\"}";

        // when
        ResultActions result = mockMvc.perform(post("/api/trainers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(trainerService);
    }

    @Test
    void getProfile_returnsProfileWithTrainees() throws Exception {
        // given
        nodira.getTrainees().add(trainee("John", "Smith", "John.Smith"));
        when(trainerService.selectByUsername(NODIRA_LOGIN, "Nodira.Karimova")).thenReturn(nodira);

        // when
        ResultActions result = mockMvc.perform(get("/api/trainers/Nodira.Karimova")
                .header(HttpHeaders.AUTHORIZATION, NODIRA_AUTH));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.specialization.trainingType").value("Yoga"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.trainees[0].username").value("John.Smith"));
    }

    @Test
    void updateProfile_ownProfile_passesNamesAndStatus() throws Exception {
        // given
        UpdateTrainerRequest request = new UpdateTrainerRequest("Nodira", "Aliyeva", true);
        when(trainerService.updateProfile(eq(NODIRA_LOGIN), any(Trainer.class))).thenReturn(nodira);

        // when
        ResultActions result = mockMvc.perform(put("/api/trainers/Nodira.Karimova")
                .header(HttpHeaders.AUTHORIZATION, NODIRA_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isOk()).andExpect(jsonPath("$.username").value("Nodira.Karimova"));
        ArgumentCaptor<Trainer> changes = ArgumentCaptor.forClass(Trainer.class);
        verify(trainerService).updateProfile(eq(NODIRA_LOGIN), changes.capture());
        assertEquals("Aliyeva", changes.getValue().getUser().getLastName());
    }

    @Test
    void updateProfile_otherUsersProfile_returns403() throws Exception {
        // given
        String body = json(new UpdateTrainerRequest("Rustam", "Aliyev", true));

        // when
        ResultActions result = mockMvc.perform(put("/api/trainers/Rustam.Aliyev")
                .header(HttpHeaders.AUTHORIZATION, NODIRA_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isForbidden());
        verify(trainerService, never()).updateProfile(any(), any());
    }

    @Test
    void register_unknownSpecialization_returns404() throws Exception {
        // given
        TrainerRegistrationRequest request = new TrainerRegistrationRequest("Nodira", "Karimova", 999L);
        when(trainerService.create(any(Trainer.class), eq(999L)))
                .thenThrow(new NoSuchElementException("Training type with id 999 not found"));

        // when
        ResultActions result = mockMvc.perform(post("/api/trainers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Training type with id 999 not found"));
    }

    @Test
    void setStatus_passesRequestedStatusToService() throws Exception {
        // given
        ActivationRequest request = new ActivationRequest(false);

        // when
        ResultActions result = mockMvc.perform(patch("/api/trainers/Nodira.Karimova/status")
                .header(HttpHeaders.AUTHORIZATION, NODIRA_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isOk());
        verify(trainerService).setActive(NODIRA_LOGIN, false);
    }

    @Test
    void getTrainings_passesQueryParametersToService() throws Exception {
        // given
        Training training = training(trainee("John", "Smith", "John.Smith"), nodira, new TrainingType("Yoga"),
                "Morning Yoga", LocalDate.of(2026, 3, 10));
        when(trainingService.getTrainerTrainings(NODIRA_LOGIN, LocalDate.of(2026, 3, 1), null, "John.Smith"))
                .thenReturn(List.of(training));

        // when
        ResultActions result = mockMvc.perform(get("/api/trainers/Nodira.Karimova/trainings")
                .header(HttpHeaders.AUTHORIZATION, NODIRA_AUTH)
                .param("periodFrom", "2026-03-01")
                .param("traineeName", "John.Smith"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("Morning Yoga"))
                .andExpect(jsonPath("$[0].traineeName").value("John.Smith"));
    }
}
