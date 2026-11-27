package com.epam.gym.workload;

import com.epam.gym.web.logging.TransactionIdFilter;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gym CRM with workload publishing switched on, against an embedded ActiveMQ broker: what lands on the queue,
 * and that nothing is sent when the database transaction rolls back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:gym_crm_messaging;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.activemq.broker-url=vm://messaging-it?broker.persistent=false&broker.useJmx=false",
        "gym.workload.enabled=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WorkloadMessagingIntegrationTest {

    private static final String QUEUE = "trainer.workload";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private TrainerWorkloadPublisher publisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Value("${gym.workload.jwt.secret}")
    private String serviceSecret;

    private JmsTemplate queueReader;

    @BeforeEach
    void drainQueue() {
        queueReader = new JmsTemplate(connectionFactory);
        queueReader.setReceiveTimeout(300);
        while (queueReader.receive(QUEUE) != null) {
            // leftovers of other tests
        }
        queueReader.setReceiveTimeout(3000);
    }

    @Test
    void addTraining_queuesJsonMessageWithTokenTransactionIdAndGroup() throws Exception {
        // given
        Account trainer = registerTrainer("Queue", "Coach");
        Account trainee = registerTrainee("Queue", "Trainee");

        // when
        addTraining(trainee, trainer, "2026-11-10", "it-tx-1").andExpect(status().isOk());

        // then
        TextMessage message = (TextMessage) queueReader.receive(QUEUE);
        String json = message.getText();
        assertEquals(trainer.username(), JsonPath.read(json, "$.trainerUsername"));
        assertEquals("Queue", JsonPath.read(json, "$.trainerFirstName"));
        assertEquals(Boolean.TRUE, JsonPath.read(json, "$.isActive"));
        assertEquals("2026-11-10", JsonPath.read(json, "$.trainingDate"));
        assertEquals(60, (int) JsonPath.read(json, "$.trainingDuration"));
        assertEquals("ADD", JsonPath.read(json, "$.actionType"));

        assertEquals("trainerWorkload", message.getStringProperty(WorkloadMessageHeaders.TYPE_ID));
        assertEquals("it-tx-1", message.getStringProperty(WorkloadMessageHeaders.TRANSACTION_ID));
        assertEquals(trainer.username(), message.getStringProperty(WorkloadMessageHeaders.GROUP_ID));
        Claims token = parseServiceToken(message.getStringProperty(WorkloadMessageHeaders.AUTHORIZATION));
        assertEquals("gym-crm", token.getIssuer());
        assertTrue(token.getAudience().contains("trainer-workload-service"));
    }

    @Test
    void deleteTrainee_queuesDeleteForEachOfItsTrainings() throws Exception {
        // given
        Account trainer = registerTrainer("Delete", "Coach");
        Account trainee = registerTrainee("Delete", "Trainee");
        addTraining(trainee, trainer, "2026-11-10", null).andExpect(status().isOk());
        addTraining(trainee, trainer, "2026-12-01", null).andExpect(status().isOk());
        drainQueue();

        // when
        mockMvc.perform(delete("/api/trainees/" + trainee.username()).header(HttpHeaders.AUTHORIZATION, trainee.auth()))
                .andExpect(status().isOk());

        // then
        List<String> received = receiveAll();
        assertEquals(List.of("DELETE", "DELETE"),
                received.stream().map(json -> JsonPath.<String>read(json, "$.actionType")).toList());
        assertEquals(List.of("2026-11-10", "2026-12-01"),
                received.stream().map(json -> JsonPath.<String>read(json, "$.trainingDate")).sorted().toList());
    }

    @Test
    void rolledBackTransaction_sendsNothing() {
        // given
        TrainerWorkloadMessage message = new TrainerWorkloadMessage("Rollback.Coach", "Rollback", "Coach", true,
                LocalDate.of(2026, 11, 10), 60, ActionType.ADD);

        // when
        assertThrows(IllegalStateException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            publisher.publish(message);
            throw new IllegalStateException("database failure after publishing");
        }));

        // then
        assertNull(queueReader.receive(QUEUE));
    }

    private List<String> receiveAll() throws JMSException {
        List<String> bodies = new ArrayList<>();
        Message message;
        while ((message = queueReader.receive(QUEUE)) != null) {
            bodies.add(((TextMessage) message).getText());
            queueReader.setReceiveTimeout(500);
        }
        return bodies;
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

    record Account(String username, String token) {
        String auth() {
            return "Bearer " + token;
        }
    }
}
