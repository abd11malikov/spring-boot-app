package com.epam.gym.workload;

import com.jayway.jsonpath.JsonPath;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gym CRM with a broker URL nothing listens on: operations that must queue a workload message answer 503,
 * change nothing, and the circuit breaker opens after repeated failures.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:gym_crm_broker_down;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.activemq.broker-url=tcp://localhost:1?connectionTimeout=500",
        "gym.workload.enabled=true",
        "resilience4j.circuitbreaker.instances.trainerWorkload.sliding-window-size=3",
        "resilience4j.circuitbreaker.instances.trainerWorkload.minimum-number-of-calls=3"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BrokerUnavailableIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void brokerDown_addTrainingAnd503_nothingSaved_thenCircuitOpens() throws Exception {
        // given
        WorkloadMessagingIntegrationTest.Account trainer = register("/api/trainers",
                "{\"firstName\":\"Down\",\"lastName\":\"Coach\",\"specializationId\":1}");
        WorkloadMessagingIntegrationTest.Account trainee = register("/api/trainees",
                "{\"firstName\":\"Down\",\"lastName\":\"Trainee\"}");

        // when
        ResultActions first = addTraining(trainee, trainer);

        // then
        first.andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
                .andExpect(jsonPath("$.message")
                        .value("Trainer workload updates are temporarily unavailable, please try again later"));
        mockMvc.perform(get("/api/trainees/" + trainee.username() + "/trainings")
                        .header(HttpHeaders.AUTHORIZATION, trainee.auth()))
                .andExpect(jsonPath("$.length()").value(0));

        // when: more failures open the circuit
        addTraining(trainee, trainer).andExpect(status().isServiceUnavailable());
        addTraining(trainee, trainer).andExpect(status().isServiceUnavailable());

        // then
        assertEquals(CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker(JmsTrainerWorkloadPublisher.CIRCUIT_BREAKER).getState());
        addTraining(trainee, trainer).andExpect(status().isServiceUnavailable());
    }

    @Test
    void brokerDown_traineeWithoutTrainings_canStillBeDeleted() throws Exception {
        // given
        WorkloadMessagingIntegrationTest.Account trainee = register("/api/trainees",
                "{\"firstName\":\"Lonely\",\"lastName\":\"Trainee\"}");

        // when / then: nothing to tell the workload service, so the broker is not needed
        mockMvc.perform(delete("/api/trainees/" + trainee.username()).header(HttpHeaders.AUTHORIZATION, trainee.auth()))
                .andExpect(status().isOk());
    }

    private ResultActions addTraining(WorkloadMessagingIntegrationTest.Account trainee,
                                      WorkloadMessagingIntegrationTest.Account trainer) throws Exception {
        return mockMvc.perform(post("/api/trainings")
                .header(HttpHeaders.AUTHORIZATION, trainee.auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"traineeUsername\":\"" + trainee.username() + "\",\"trainerUsername\":\""
                        + trainer.username() + "\",\"trainingName\":\"Session\",\"trainingDate\":\"2026-11-10\","
                        + "\"trainingDuration\":60}"));
    }

    private WorkloadMessagingIntegrationTest.Account register(String url, String body) throws Exception {
        String json = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new WorkloadMessagingIntegrationTest.Account(JsonPath.read(json, "$.username"),
                JsonPath.read(json, "$.token"));
    }
}
