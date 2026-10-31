package com.epam.gym.web.controller;

import com.epam.gym.dto.Credentials;
import com.epam.gym.model.TrainingType;
import com.epam.gym.service.TrainingTypeService;
import com.epam.gym.web.mapper.TrainingTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainingTypeControllerTest {

    @Mock
    private TrainingTypeService trainingTypeService;

    private static final Credentials LOGIN = new Credentials("John.Smith", "password12");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TrainingTypeController controller = new TrainingTypeController(trainingTypeService, new TrainingTypeMapper());
        mockMvc = ControllerTestSupport.mockMvc(controller);
    }

    @Test
    void getTrainingTypes_returnsNameAndIdOfEachType() throws Exception {
        // given
        when(trainingTypeService.findAll(LOGIN)).thenReturn(List.of(new TrainingType("Yoga"), new TrainingType("Cardio")));

        // when
        ResultActions result = mockMvc.perform(get("/api/training-types")
                .header(HttpHeaders.AUTHORIZATION, ControllerTestSupport.basicAuth("John.Smith", "password12")));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].trainingType").value("Yoga"))
                .andExpect(jsonPath("$[1].trainingType").value("Cardio"));
    }

    @Test
    void getTrainingTypes_noTypes_returnsEmptyList() throws Exception {
        // given
        when(trainingTypeService.findAll(LOGIN)).thenReturn(List.of());

        // when
        ResultActions result = mockMvc.perform(get("/api/training-types")
                .header(HttpHeaders.AUTHORIZATION, ControllerTestSupport.basicAuth("John.Smith", "password12")));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
