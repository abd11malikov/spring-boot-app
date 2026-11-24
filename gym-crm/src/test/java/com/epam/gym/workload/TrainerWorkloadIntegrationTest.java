package com.epam.gym.workload;

import com.epam.gym.web.logging.TransactionIdFilter;
import com.jayway.jsonpath.JsonPath;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gym CRM with the workload integration switched on, against a stub workload service registered under its
 * service name (simple discovery instead of Eureka): the load-balanced call, service token, transaction id,
 * timeouts, rollback and circuit breaker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:gym_crm_workload;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "gym.workload.enabled=true",
        "gym.workload.connect-timeout=500ms",
        "gym.workload.read-timeout=500ms",
        "resilience4j.circuitbreaker.instances.trainerWorkload.sliding-window-size=3",
        "resilience4j.circuitbreaker.instances.trainerWorkload.minimum-number-of-calls=3"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TrainerWorkloadIntegrationTest {

    private static final StubWorkloadServer STUB = StubWorkloadServer.start();

    @DynamicPropertySource
    static void registerStubUnderServiceName(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.trainer-workload-service[0].uri", STUB::uri);
    }

    @AfterAll
    static void stopStub() {
        STUB.stop();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Value("${gym.workload.jwt.secret}")
    private String serviceSecret;

    @BeforeEach
    void resetStubAndCircuit() {
        STUB.reset();
        circuitBreaker().reset();
    }

    @Test
    void addTraining_sendsAddWithServiceTokenAndSameTransactionId() throws Exception {
        // given
        Account trainer = registerTrainer("Stub", "Coach");
        Account trainee = registerTrainee("Stub", "Trainee");

        // when
        addTraining(trainee, trainer, "2026-11-10", "it-tx-1").andExpect(status().isOk());

        // then
        List<StubWorkloadServer.ReceivedRequest> requests = STUB.requests();
        assertEquals(1, requests.size());
        StubWorkloadServer.ReceivedRequest request = requests.get(0);
        assertEquals("POST", request.method());
        assertEquals("/api/trainer-workloads", request.path());
        assertEquals("it-tx-1", request.header(TransactionIdFilter.HEADER));
        assertEquals(trainer.username(), JsonPath.read(request.body(), "$.trainerUsername"));
        assertEquals("Stub", JsonPath.read(request.body(), "$.trainerFirstName"));
        assertEquals(Boolean.TRUE, JsonPath.read(request.body(), "$.isActive"));
        assertEquals("2026-11-10", JsonPath.read(request.body(), "$.trainingDate"));
        assertEquals(60, (int) JsonPath.read(request.body(), "$.trainingDuration"));
        assertEquals("ADD", JsonPath.read(request.body(), "$.actionType"));

        Claims token = parseServiceToken(request.header(HttpHeaders.AUTHORIZATION));
        assertEquals("gym-crm", token.getIssuer());
        assertTrue(token.getAudience().contains("trainer-workload-service"));
    }

    @Test
    void deleteTrainee_sendsDeleteForEachOfItsTrainings() throws Exception {
        // given
        Account trainer = registerTrainer("Delete", "Coach");
        Account trainee = registerTrainee("Delete", "Trainee");
        addTraining(trainee, trainer, "2026-11-10", null).andExpect(status().isOk());
        addTraining(trainee, trainer, "2026-12-01", null).andExpect(status().isOk());
        STUB.reset();

        // when
        mockMvc.perform(delete("/api/trainees/" + trainee.username()).header(HttpHeaders.AUTHORIZATION, trainee.auth()))
                .andExpect(status().isOk());

        // then
        List<String> actions = STUB.requests().stream()
                .map(request -> JsonPath.<String>read(request.body(), "$.actionType")).toList();
        List<String> dates = STUB.requests().stream()
                .map(request -> JsonPath.<String>read(request.body(), "$.trainingDate")).sorted().toList();
        assertEquals(List.of("DELETE", "DELETE"), actions);
        assertEquals(List.of("2026-11-10", "2026-12-01"), dates);
    }

    @Test
    void workloadServiceError_returns503AndDoesNotSaveTraining() throws Exception {
        // given
        Account trainer = registerTrainer("Error", "Coach");
        Account trainee = registerTrainee("Error", "Trainee");
        STUB.respondWith(500, Duration.ZERO);

        // when
        ResultActions result = addTraining(trainee, trainer, "2026-11-10", null);

        // then
        result.andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Trainer workload service is temporarily unavailable, please try again later"));
        mockMvc.perform(get("/api/trainees/" + trainee.username() + "/trainings")
                        .header(HttpHeaders.AUTHORIZATION, trainee.auth()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void slowWorkloadService_timesOutQuicklyWith503() throws Exception {
        // given
        Account trainer = registerTrainer("Slow", "Coach");
        Account trainee = registerTrainee("Slow", "Trainee");
        STUB.respondWith(200, Duration.ofSeconds(3));

        // when
        long start = System.nanoTime();
        ResultActions result = addTraining(trainee, trainer, "2026-11-10", null);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        // then
        result.andExpect(status().isServiceUnavailable());
        assertTrue(elapsed.compareTo(Duration.ofSeconds(2)) < 0, "Timed out after " + elapsed);
    }

    @Test
    void repeatedFailures_openCircuit_soServiceIsNoLongerCalled() throws Exception {
        // given
        Account trainer = registerTrainer("Open", "Circuit");
        Account trainee = registerTrainee("Open", "Circuit");
        STUB.respondWith(500, Duration.ZERO);
        for (int i = 0; i < 3; i++) {
            addTraining(trainee, trainer, "2026-11-10", null).andExpect(status().isServiceUnavailable());
        }

        // when
        ResultActions result = addTraining(trainee, trainer, "2026-11-10", null);

        // then
        result.andExpect(status().isServiceUnavailable());
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker().getState());
        assertEquals(3, STUB.requests().size());
    }

    @Test
    void deleteTrainee_workloadServiceDown_keepsTraineeAndTrainings() throws Exception {
        // given
        Account trainer = registerTrainer("Keep", "Coach");
        Account trainee = registerTrainee("Keep", "Trainee");
        addTraining(trainee, trainer, "2026-11-10", null).andExpect(status().isOk());
        STUB.respondWith(500, Duration.ZERO);

        // when
        ResultActions result = mockMvc.perform(delete("/api/trainees/" + trainee.username())
                .header(HttpHeaders.AUTHORIZATION, trainee.auth()));

        // then
        result.andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/trainees/" + trainee.username() + "/trainings")
                        .header(HttpHeaders.AUTHORIZATION, trainee.auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private CircuitBreaker circuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker(RestTrainerWorkloadClient.CIRCUIT_BREAKER);
    }

    private Claims parseServiceToken(String authorization) {
        assertTrue(authorization.startsWith("Bearer "));
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(serviceSecret)))
                .build()
                .parseSignedClaims(authorization.substring("Bearer ".length()))
                .getPayload();
    }

    private ResultActions addTraining(Account trainee, Account trainer, String date, String transactionId)
            throws Exception {
        var request = post("/api/trainings")
                .header(HttpHeaders.AUTHORIZATION, trainee.auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"traineeUsername\":\"" + trainee.username() + "\",\"trainerUsername\":\""
                        + trainer.username() + "\",\"trainingName\":\"Session\",\"trainingDate\":\"" + date
                        + "\",\"trainingDuration\":60}");
        if (transactionId != null) {
            request.header(TransactionIdFilter.HEADER, transactionId);
        }
        return mockMvc.perform(request);
    }

    private Account registerTrainee(String firstName, String lastName) throws Exception {
        return register("/api/trainees", "{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\"}");
    }

    private Account registerTrainer(String firstName, String lastName) throws Exception {
        return register("/api/trainers", "{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName
                + "\",\"specializationId\":1}");
    }

    private Account register(String url, String body) throws Exception {
        String json = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Account(JsonPath.read(json, "$.username"), JsonPath.read(json, "$.token"));
    }

    private record Account(String username, String token) {
        String auth() {
            return "Bearer " + token;
        }
    }
}
