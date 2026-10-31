package com.epam.gym.web.controller;

import com.epam.gym.dto.Credentials;
import com.epam.gym.dto.TrainingRequest;
import com.epam.gym.service.TrainingService;
import com.epam.gym.web.dto.request.AddTrainingRequest;
import com.epam.gym.web.mapper.TrainingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static com.epam.gym.web.controller.ControllerTestSupport.basicAuth;
import static com.epam.gym.web.controller.ControllerTestSupport.json;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainingControllerTest {

    private static final String JOHN_AUTH = basicAuth("John.Smith", "password12");

    @Mock
    private TrainingService trainingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvc(new TrainingController(trainingService, new TrainingMapper()));
    }

    @Test
    void addTraining_validRequest_returns200AndPassesRequestToService() throws Exception {
        // given
        AddTrainingRequest request = new AddTrainingRequest(
                "John.Smith", "Nodira.Karimova", "Morning Yoga", LocalDate.of(2026, 11, 10), 60);

        // when
        ResultActions result = mockMvc.perform(post("/api/trainings")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isOk());
        verify(trainingService).addTraining(new Credentials("John.Smith", "password12"), new TrainingRequest(
                "John.Smith", "Nodira.Karimova", "Morning Yoga", LocalDate.of(2026, 11, 10), 60));
    }

    @Test
    void addTraining_negativeDuration_returns400() throws Exception {
        // given
        AddTrainingRequest request = new AddTrainingRequest(
                "John.Smith", "Nodira.Karimova", "Morning Yoga", LocalDate.of(2026, 11, 10), -5);

        // when
        ResultActions result = mockMvc.perform(post("/api/trainings")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(trainingService);
    }
}
