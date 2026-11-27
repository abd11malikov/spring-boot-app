package com.epam.trainerworkload.messaging;

import com.epam.trainerworkload.MongoTestContainer;
import com.epam.trainerworkload.ServiceTokens;
import com.epam.trainerworkload.repository.TrainerWorkloadRepository;
import com.epam.trainerworkload.service.TrainerWorkloadService;
import com.epam.trainerworkload.service.TrainerWorkloadUpdate;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;

/**
 * The listener on a real (embedded) ActiveMQ broker: messages update workloads, invalid ones and ones that keep
 * failing end in the dead letter queue, transient failures are redelivered.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(MongoTestContainer.class)
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WorkloadMessagingIntegrationTest {

    private static final String QUEUE = "trainer.workload";
    private static final String DLQ = "DLQ.trainer.workload";

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private TrainerWorkloadRepository repository;

    @MockitoSpyBean
    private TrainerWorkloadService service;

    private JmsTemplate jms;

    @BeforeEach
    void setUp() {
        jms = new JmsTemplate(connectionFactory);
        jms.setReceiveTimeout(200);
        while (jms.receive(DLQ) != null) {
            // leftovers of other tests
        }
        jms.setReceiveTimeout(5000);
    }

    @Test
    void validMessages_updateWorkload() {
        // when
        send(json("Jms.Trainer", "2026-11-10", 60, "ADD"), ServiceTokens.valid(), "jms-tx-1");
        send(json("Jms.Trainer", "2026-11-11", 30, "ADD"), ServiceTokens.valid(), "jms-tx-2");
        send(json("Jms.Trainer", "2026-11-10", 60, "DELETE"), ServiceTokens.valid(), "jms-tx-3");

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertEquals(30,
                repository.findByUsername("Jms.Trainer").orElseThrow().getMonthlyDuration(2026, 11)));
    }

    @Test
    void messageMissingRequiredInformation_goesToDeadLetterQueueWithReason() throws JMSException {
        // when
        send("{\"trainerUsername\":\"Broken.Trainer\",\"trainingDuration\":60}", ServiceTokens.valid(), "jms-tx-dlq");

        // then
        TextMessage deadLetter = (TextMessage) jms.receive(DLQ);
        assertTrue(deadLetter.getStringProperty(WorkloadMessageHeaders.DEAD_LETTER_REASON).startsWith("Invalid fields:"));
        assertEquals(QUEUE, deadLetter.getStringProperty(WorkloadMessageHeaders.ORIGINAL_QUEUE));
        assertEquals("jms-tx-dlq", deadLetter.getStringProperty(WorkloadMessageHeaders.TRANSACTION_ID));
        assertNull(deadLetter.getStringProperty(WorkloadMessageHeaders.AUTHORIZATION));
        assertTrue(deadLetter.getText().contains("Broken.Trainer"));
        assertTrue(repository.findByUsername("Broken.Trainer").isEmpty());
    }

    @Test
    void messageWithoutToken_goesToDeadLetterQueue() throws JMSException {
        // when
        send(json("Anonymous.Trainer", "2026-11-10", 60, "ADD"), null, null);

        // then
        TextMessage deadLetter = (TextMessage) jms.receive(DLQ);
        assertEquals("Missing service token", deadLetter.getStringProperty(WorkloadMessageHeaders.DEAD_LETTER_REASON));
        assertTrue(repository.findByUsername("Anonymous.Trainer").isEmpty());
    }

    @Test
    void transientFailure_isRedeliveredAndThenSucceeds() {
        // given: the first attempt fails
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("temporary failure");
            }
            return invocation.callRealMethod();
        }).when(service).apply(argThat((TrainerWorkloadUpdate update) -> "Flaky.Trainer".equals(update.trainerUsername())));

        // when
        send(json("Flaky.Trainer", "2026-11-10", 45, "ADD"), ServiceTokens.valid(), "jms-tx-flaky");

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertEquals(45,
                repository.findByUsername("Flaky.Trainer").orElseThrow().getMonthlyDuration(2026, 11)));
        assertEquals(2, attempts.get());
    }

    @Test
    void persistentFailure_endsInDeadLetterQueueAfterMaxAttempts() throws JMSException {
        // given
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("always failing");
        }).when(service).apply(argThat((TrainerWorkloadUpdate update) -> "Doomed.Trainer".equals(update.trainerUsername())));

        // when
        send(json("Doomed.Trainer", "2026-11-10", 45, "ADD"), ServiceTokens.valid(), "jms-tx-doomed");

        // then
        TextMessage deadLetter = (TextMessage) jms.receive(DLQ);
        assertEquals("Processing failed after 4 attempts: always failing",
                deadLetter.getStringProperty(WorkloadMessageHeaders.DEAD_LETTER_REASON));
        assertEquals(4, attempts.get());
    }

    /**
     * Sends a message the way Gym CRM does, grouped by trainer so one trainer's messages are handled in order.
     */
    private void send(String payload, String authorization, String transactionId) {
        String trainer = payload.replaceAll(".*\"trainerUsername\":\"([^\"]*)\".*", "$1");
        jms.convertAndSend(QUEUE, payload, message -> {
            message.setStringProperty("JMSXGroupID", trainer);
            if (authorization != null) {
                message.setStringProperty(WorkloadMessageHeaders.AUTHORIZATION, authorization);
            }
            if (transactionId != null) {
                message.setStringProperty(WorkloadMessageHeaders.TRANSACTION_ID, transactionId);
            }
            message.setStringProperty(WorkloadMessageHeaders.TYPE_ID, "trainerWorkload");
            return message;
        });
    }

    private static String json(String username, String date, int duration, String action) {
        return "{\"trainerUsername\":\"" + username + "\",\"trainerFirstName\":\"Nodira\","
                + "\"trainerLastName\":\"Karimova\",\"isActive\":true,\"trainingDate\":\"" + date + "\","
                + "\"trainingDuration\":" + duration + ",\"actionType\":\"" + action + "\"}";
    }
}
