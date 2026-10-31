package com.epam.gym.e2e;

import com.epam.gym.config.AppConfig;
import com.epam.gym.web.config.WebConfig;
import com.epam.gym.web.logging.RestCallLoggingFilter;
import com.epam.gym.web.logging.TransactionIdFilter;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig
@ContextHierarchy({
        @ContextConfiguration(classes = AppConfig.class),
        @ContextConfiguration(classes = WebConfig.class)
})
@TestPropertySource(locations = "classpath:application-test.properties", properties =
        "db.url=jdbc:h2:mem:gym_crm_e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GymCrmApiEndToEndTest {

    @Autowired
    private WebApplicationContext webContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext)
                .addFilter(new TransactionIdFilter())
                .addFilter(new RestCallLoggingFilter(2000), "/api/*")
                .build();
    }

    @Test
    void traineeAndTrainerJourney_coversEveryEndpoint() throws Exception {
        // given: a trainer and a trainee register
        Account trainer = registerTrainer("Nodira", "Karimova", trainingTypeId("Yoga"));
        Account trainee = registerTrainee("John", "Doe");

        // when / then: login works only with the right password
        mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, trainee.auth()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, basic(trainee.username(), "wrong")))
                .andExpect(status().isUnauthorized());

        // when / then: the new trainee has no trainers and sees the trainer as unassigned
        mockMvc.perform(get("/api/trainees/" + trainee.username()).header(HttpHeaders.AUTHORIZATION, trainee.auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.trainers.length()").value(0));
        mockMvc.perform(get("/api/trainees/" + trainee.username() + "/unassigned-trainers")
                        .header(HttpHeaders.AUTHORIZATION, trainee.auth()))
                .andExpect(jsonPath("$[*].username", hasItem(trainer.username())));

        // when / then: assigning the trainer removes it from the unassigned list
        mockMvc.perform(put("/api/trainees/" + trainee.username() + "/trainers")
                        .header(HttpHeaders.AUTHORIZATION, trainee.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainerUsernames\":[\"" + trainer.username() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(trainer.username()))
                .andExpect(jsonPath("$[0].specialization.trainingType").value("Yoga"));
        mockMvc.perform(get("/api/trainees/" + trainee.username() + "/unassigned-trainers")
                        .header(HttpHeaders.AUTHORIZATION, trainee.auth()))
                .andExpect(jsonPath("$[*].username", not(hasItem(trainer.username()))));

        // when / then: a training added by the trainee is found by both sides' search
        addTraining(trainee, trainee.username(), trainer.username(), "Morning Yoga", "2026-11-10")
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/trainees/" + trainee.username() + "/trainings")
                        .header(HttpHeaders.AUTHORIZATION, trainee.auth())
                        .param("periodFrom", "2026-11-01")
                        .param("trainingType", "Yoga"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trainingName").value("Morning Yoga"))
                .andExpect(jsonPath("$[0].trainingType").value("Yoga"))
                .andExpect(jsonPath("$[0].trainerName").value(trainer.username()));
        mockMvc.perform(get("/api/trainers/" + trainer.username() + "/trainings")
                        .header(HttpHeaders.AUTHORIZATION, trainer.auth())
                        .param("traineeName", trainee.username()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].traineeName").value(trainee.username()));

        // when / then: the trainer profile lists the trainee
        mockMvc.perform(get("/api/trainers/" + trainer.username()).header(HttpHeaders.AUTHORIZATION, trainer.auth()))
                .andExpect(jsonPath("$.specialization.trainingType").value("Yoga"))
                .andExpect(jsonPath("$.trainees[0].username").value(trainee.username()));

        // when / then: updating the trainee keeps the username and returns the trainer list
        mockMvc.perform(put("/api/trainees/" + trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, trainee.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Johnny\",\"lastName\":\"Doe\",\"dateOfBirth\":\"1998-03-15\","
                                + "\"address\":\"Samarkand\",\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(trainee.username()))
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.dateOfBirth").value("1998-03-15"))
                .andExpect(jsonPath("$.trainers[0].username").value(trainer.username()));

        // when / then: the trainer updates their profile but cannot change specialization
        mockMvc.perform(put("/api/trainers/" + trainer.username())
                        .header(HttpHeaders.AUTHORIZATION, trainer.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Nodira\",\"lastName\":\"Aliyeva\",\"isActive\":true,"
                                + "\"specializationId\":" + trainingTypeId("Cardio") + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Aliyeva"))
                .andExpect(jsonPath("$.specialization.trainingType").value("Yoga"));

        // when / then: the password change takes effect
        mockMvc.perform(put("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + trainee.username() + "\",\"oldPassword\":\""
                                + trainee.password() + "\",\"newPassword\":\"NewSecret99\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, trainee.auth()))
                .andExpect(status().isUnauthorized());
        String newAuth = basic(trainee.username(), "NewSecret99");
        mockMvc.perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, newAuth))
                .andExpect(status().isOk());

        // when / then: deleting the trainee also deletes the trainings
        mockMvc.perform(delete("/api/trainees/" + trainee.username()).header(HttpHeaders.AUTHORIZATION, newAuth))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/trainers/" + trainer.username() + "/trainings")
                        .header(HttpHeaders.AUTHORIZATION, trainer.auth()))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/trainees/" + trainee.username()).header(HttpHeaders.AUTHORIZATION, trainer.auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void registeringSameNameTwice_addsSerialNumber() throws Exception {
        // given
        Account first = registerTrainee("Ali", "Valiyev");

        // when
        Account second = registerTrainee("Ali", "Valiyev");

        // then
        assertEquals(first.username() + "1", second.username());
        assertEquals(10, second.password().length());
    }

    @Test
    void statusChange_isNotIdempotent() throws Exception {
        // given
        Account trainer = registerTrainer("Rustam", "Aliyev", trainingTypeId("Cardio"));
        String deactivate = "{\"isActive\":false}";
        mockMvc.perform(patch("/api/trainers/" + trainer.username() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, trainer.auth())
                        .contentType(MediaType.APPLICATION_JSON).content(deactivate))
                .andExpect(status().isOk());

        // when
        ResultActions secondCall = mockMvc.perform(patch("/api/trainers/" + trainer.username() + "/status")
                .header(HttpHeaders.AUTHORIZATION, trainer.auth())
                .contentType(MediaType.APPLICATION_JSON).content(deactivate));

        // then
        secondCall.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Trainer " + trainer.username() + " is already inactive"));
    }

    @Test
    void inactiveTrainer_isNotOfferedAsUnassigned() throws Exception {
        // given
        Account trainee = registerTrainee("Lola", "Rahimova");
        Account trainer = registerTrainer("Inactive", "Coach", trainingTypeId("Zumba"));
        mockMvc.perform(patch("/api/trainers/" + trainer.username() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, trainer.auth())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"isActive\":false}"))
                .andExpect(status().isOk());

        // when
        ResultActions result = mockMvc.perform(get("/api/trainees/" + trainee.username() + "/unassigned-trainers")
                .header(HttpHeaders.AUTHORIZATION, trainee.auth()));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", not(hasItem(trainer.username()))));
    }

    @Test
    void addingTrainingForSomeoneElse_isForbidden() throws Exception {
        // given
        Account trainer = registerTrainer("Busy", "Coach", trainingTypeId("Fitness"));
        Account trainee = registerTrainee("Real", "Trainee");
        Account stranger = registerTrainee("Nosy", "Stranger");

        // when
        ResultActions result = addTraining(stranger, trainee.username(), trainer.username(), "Sneaky", "2026-11-11");

        // then
        result.andExpect(status().isForbidden())
                .andExpect(header().exists(TransactionIdFilter.HEADER))
                .andExpect(jsonPath("$.transactionId").exists());
    }

    @Test
    void everyResponse_carriesTransactionIdHeader() throws Exception {
        // given
        String incomingId = "e2e-trace-1";

        // when
        ResultActions result = mockMvc.perform(post("/api/trainees")
                .header(TransactionIdFilter.HEADER, incomingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Trace\",\"lastName\":\"Me\"}"));

        // then
        result.andExpect(status().isCreated())
                .andExpect(header().string(TransactionIdFilter.HEADER, incomingId));
    }

    private Account registerTrainee(String firstName, String lastName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/trainees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return account(result);
    }

    private Account registerTrainer(String firstName, String lastName, int specializationId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/trainers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName
                                + "\",\"specializationId\":" + specializationId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return account(result);
    }

    private int trainingTypeId(String name) throws Exception {
        Account reader = registerTrainee("Type", "Reader");
        String json = mockMvc.perform(get("/api/training-types").header(HttpHeaders.AUTHORIZATION, reader.auth()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(json, "$[?(@.trainingType == '" + name + "')].id");
        return ids.get(0);
    }

    private ResultActions addTraining(Account caller, String traineeUsername, String trainerUsername,
                                      String name, String date) throws Exception {
        return mockMvc.perform(post("/api/trainings")
                .header(HttpHeaders.AUTHORIZATION, caller.auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"traineeUsername\":\"" + traineeUsername + "\",\"trainerUsername\":\"" + trainerUsername
                        + "\",\"trainingName\":\"" + name + "\",\"trainingDate\":\"" + date
                        + "\",\"trainingDuration\":60}"));
    }

    private Account account(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return new Account(JsonPath.read(json, "$.username"), JsonPath.read(json, "$.password"));
    }

    private static String basic(String username, String password) {
        String token = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private record Account(String username, String password) {
        String auth() {
            return basic(username, password);
        }
    }
}
