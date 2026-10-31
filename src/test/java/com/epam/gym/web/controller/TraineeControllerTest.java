package com.epam.gym.web.controller;

import com.epam.gym.dto.Credentials;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.Training;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.User;
import com.epam.gym.service.TraineeService;
import com.epam.gym.service.TrainingService;
import com.epam.gym.web.dto.request.ActivationRequest;
import com.epam.gym.web.dto.request.TraineeRegistrationRequest;
import com.epam.gym.web.dto.request.UpdateTraineeRequest;
import com.epam.gym.web.dto.request.UpdateTraineeTrainersRequest;
import com.epam.gym.web.mapper.SummaryMapper;
import com.epam.gym.web.mapper.TraineeMapper;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TraineeControllerTest {

    private static final Credentials JOHN_LOGIN = new Credentials("John.Smith", "password12");
    private static final String JOHN_AUTH = basicAuth("John.Smith", "password12");

    @Mock
    private TraineeService traineeService;

    @Mock
    private TrainingService trainingService;

    private MockMvc mockMvc;

    private Trainee john;
    private Trainer nodira;

    @BeforeEach
    void setUp() {
        SummaryMapper summaryMapper = new SummaryMapper(new TrainingTypeMapper());
        TraineeController controller = new TraineeController(traineeService, trainingService,
                new TraineeMapper(summaryMapper), summaryMapper, new TrainingMapper());
        mockMvc = ControllerTestSupport.mockMvc(controller);

        john = trainee("John", "Smith", "John.Smith");
        nodira = trainer("Nodira", "Karimova", "Nodira.Karimova", new TrainingType("Yoga"));
    }

    @Test
    void register_validRequest_returns201WithCredentials() throws Exception {
        // given
        TraineeRegistrationRequest request =
                new TraineeRegistrationRequest("John", "Smith", LocalDate.of(2000, 1, 1), "Tashkent");
        when(traineeService.create(any(Trainee.class))).thenReturn(john);

        // when
        ResultActions result = mockMvc.perform(post("/api/trainees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("John.Smith"))
                .andExpect(jsonPath("$.password").value("password12"));
    }

    @Test
    void register_missingLastName_returns400AndDoesNotCallService() throws Exception {
        // given
        String body = "{\"firstName\":\"John\"}";

        // when
        ResultActions result = mockMvc.perform(post("/api/trainees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(traineeService);
    }

    @Test
    void getProfile_returnsProfileWithTrainers() throws Exception {
        // given
        john.getTrainers().add(nodira);
        when(traineeService.selectByUsername(JOHN_LOGIN, "John.Smith")).thenReturn(john);

        // when
        ResultActions result = mockMvc.perform(get("/api/trainees/John.Smith")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.dateOfBirth").value("2000-01-01"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.trainers[0].username").value("Nodira.Karimova"))
                .andExpect(jsonPath("$.trainers[0].specialization.trainingType").value("Yoga"));
    }

    @Test
    void getProfile_withoutAuthorizationHeader_returns401() throws Exception {
        // given
        String url = "/api/trainees/John.Smith";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing or invalid Authorization header"));
        verifyNoInteractions(traineeService);
    }

    @Test
    void getProfile_unknownTrainee_returns404() throws Exception {
        // given
        when(traineeService.selectByUsername(JOHN_LOGIN, "Nobody.Here"))
                .thenThrow(new NoSuchElementException("Trainee Nobody.Here not found"));

        // when
        ResultActions result = mockMvc.perform(get("/api/trainees/Nobody.Here")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Trainee Nobody.Here not found"));
    }

    @Test
    void updateProfile_ownProfile_passesChangesToService() throws Exception {
        // given
        UpdateTraineeRequest request = new UpdateTraineeRequest("Johnny", "Smith", null, "Samarkand", false);
        when(traineeService.updateProfile(eq(JOHN_LOGIN), any(Trainee.class))).thenReturn(john);

        // when
        ResultActions result = mockMvc.perform(put("/api/trainees/John.Smith")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isOk()).andExpect(jsonPath("$.username").value("John.Smith"));
        ArgumentCaptor<Trainee> changes = ArgumentCaptor.forClass(Trainee.class);
        verify(traineeService).updateProfile(eq(JOHN_LOGIN), changes.capture());
        assertEquals("Johnny", changes.getValue().getUser().getFirstName());
        assertEquals("Samarkand", changes.getValue().getAddress());
        assertFalse(changes.getValue().getUser().isActive());
    }

    @Test
    void updateProfile_missingIsActive_returns400NamingTheField() throws Exception {
        // given
        String body = "{\"firstName\":\"John\",\"lastName\":\"Smith\"}";

        // when
        ResultActions result = mockMvc.perform(put("/api/trainees/John.Smith")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("isActive"));
        verifyNoInteractions(traineeService);
    }

    @Test
    void updateProfile_otherUsersProfile_returns403() throws Exception {
        // given
        String body = json(new UpdateTraineeRequest("Jane", "Doe", null, null, true));

        // when
        ResultActions result = mockMvc.perform(put("/api/trainees/Jane.Doe")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only access your own profile"));
        verify(traineeService, never()).updateProfile(any(), any());
    }

    @Test
    void delete_ownProfile_callsService() throws Exception {
        // given
        String url = "/api/trainees/John.Smith";

        // when
        ResultActions result = mockMvc.perform(delete(url).header(HttpHeaders.AUTHORIZATION, JOHN_AUTH));

        // then
        result.andExpect(status().isOk());
        verify(traineeService).delete(JOHN_LOGIN);
    }

    @Test
    void setStatus_sameStatusTwice_returns409() throws Exception {
        // given
        doThrow(new IllegalStateException("Trainee John.Smith is already active"))
                .when(traineeService).setActive(JOHN_LOGIN, true);

        // when
        ResultActions result = mockMvc.perform(patch("/api/trainees/John.Smith/status")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new ActivationRequest(true))));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Trainee John.Smith is already active"));
    }

    @Test
    void setStatus_passesRequestedStatusToService() throws Exception {
        // given
        ActivationRequest request = new ActivationRequest(false);

        // when
        ResultActions result = mockMvc.perform(patch("/api/trainees/John.Smith/status")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isOk());
        verify(traineeService).setActive(JOHN_LOGIN, false);
    }

    @Test
    void getUnassignedTrainers_returnsTrainerSummaries() throws Exception {
        // given
        when(traineeService.getUnassignedTrainers(JOHN_LOGIN)).thenReturn(List.of(nodira));

        // when
        ResultActions result = mockMvc.perform(get("/api/trainees/John.Smith/unassigned-trainers")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("Nodira.Karimova"))
                .andExpect(jsonPath("$[0].firstName").value("Nodira"));
    }

    @Test
    void updateTrainers_returnsNewTrainersList() throws Exception {
        // given
        UpdateTraineeTrainersRequest request = new UpdateTraineeTrainersRequest(List.of("Nodira.Karimova"));
        when(traineeService.updateTrainers(JOHN_LOGIN, List.of("Nodira.Karimova"))).thenReturn(List.of(nodira));

        // when
        ResultActions result = mockMvc.perform(put("/api/trainees/John.Smith/trainers")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("Nodira.Karimova"));
    }

    @Test
    void getTrainings_passesQueryParametersToService() throws Exception {
        // given
        Training training = training(john, nodira, new TrainingType("Yoga"), "Morning Yoga", LocalDate.of(2026, 3, 10));
        when(trainingService.getTraineeTrainings(JOHN_LOGIN, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                "Nodira.Karimova", "Yoga")).thenReturn(List.of(training));

        // when
        ResultActions result = mockMvc.perform(get("/api/trainees/John.Smith/trainings")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH)
                .param("periodFrom", "2026-03-01")
                .param("periodTo", "2026-03-31")
                .param("trainerName", "Nodira.Karimova")
                .param("trainingType", "Yoga"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("Morning Yoga"))
                .andExpect(jsonPath("$[0].trainingDate").value("2026-03-10"))
                .andExpect(jsonPath("$[0].trainingType").value("Yoga"))
                .andExpect(jsonPath("$[0].trainingDuration").value(60))
                .andExpect(jsonPath("$[0].trainerName").value("Nodira.Karimova"));
    }

    @Test
    void getTrainings_withoutFilters_passesNulls() throws Exception {
        // given
        when(trainingService.getTraineeTrainings(JOHN_LOGIN, null, null, null, null)).thenReturn(List.of());

        // when
        ResultActions result = mockMvc.perform(get("/api/trainees/John.Smith/trainings")
                .header(HttpHeaders.AUTHORIZATION, JOHN_AUTH));

        // then
        result.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void register_newTraineeHasNoUsernameBeforeService() throws Exception {
        // given
        TraineeRegistrationRequest request = new TraineeRegistrationRequest("John", "Smith", null, null);
        when(traineeService.create(any(Trainee.class))).thenReturn(new Trainee(userWithCredentials()));

        // when
        mockMvc.perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON).content(json(request)));

        // then
        ArgumentCaptor<Trainee> created = ArgumentCaptor.forClass(Trainee.class);
        verify(traineeService).create(created.capture());
        assertNull(created.getValue().getUser().getUsername());
    }

    private User userWithCredentials() {
        User user = new User("John", "Smith");
        user.setUsername("John.Smith");
        user.setPassword("password12");
        return user;
    }
}
