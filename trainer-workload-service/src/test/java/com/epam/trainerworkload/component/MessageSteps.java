package com.epam.trainerworkload.component;

import com.epam.trainerworkload.ServiceTokens;
import com.epam.trainerworkload.messaging.WorkloadMessageHeaders;
import com.epam.trainerworkload.model.MonthSummary;
import com.epam.trainerworkload.model.TrainerWorkload;
import com.epam.trainerworkload.repository.TrainerWorkloadRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.springframework.jms.core.JmsTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plays Gym CRM: puts workload messages on the queue the way it does (JSON body, service token, transaction id,
 * message group) or deliberately broken ones, and checks the stored workload and the dead letter queue.
 */
public class MessageSteps {

    private static final String QUEUE = "trainer.workload";
    private static final String DEAD_LETTER_QUEUE = "DLQ.trainer.workload";
    private static final Duration PROCESSING_TIMEOUT = Duration.ofSeconds(5);

    private final JmsTemplate jms;
    private final TrainerWorkloadRepository repository;
    private final ScenarioState state;

    public MessageSteps(ConnectionFactory connectionFactory, TrainerWorkloadRepository repository,
                        ScenarioState state) {
        this.jms = new JmsTemplate(connectionFactory);
        this.repository = repository;
        this.state = state;
    }

    @Before
    public void startFromAnEmptyDatabaseAndDeadLetterQueue() {
        repository.deleteAll();
        jms.setReceiveTimeout(200);
        while (jms.receive(DEAD_LETTER_QUEUE) != null) {
            // dead letters of earlier scenarios
        }
    }

    @Given("a trainer {string} named {string} {string}")
    public void aTrainerNamed(String username, String firstName, String lastName) {
        state.rememberTrainer(username, firstName, lastName);
    }

    @Given("the trainer {string} has no workload yet")
    @Then("the trainer {string} still has no workload")
    public void theTrainerHasNoWorkload(String username) {
        assertTrue(repository.findByUsername(username).isEmpty());
    }

    @When("Gym CRM reports a(n) {word} of {int} minutes on {string} for {string}")
    public void gymCrmReports(String action, int minutes, String date, String username) {
        send(body(username, action, date, minutes), ServiceTokens.valid(), username);
    }

    @Given("Gym CRM reported a(n) {word} of {int} minutes on {string} for {string}")
    public void gymCrmReported(String action, int minutes, String date, String username) {
        long before = minutesIn(username, date);
        gymCrmReports(action, minutes, date, username);
        await().atMost(PROCESSING_TIMEOUT).until(() -> minutesIn(username, date) != before);
    }

    @When("a workload message for {string} arrives without a service token")
    public void aMessageWithoutToken(String username) {
        send(body(username, "ADD", "2026-11-10", 60), null, username);
    }

    @When("a workload message for {string} arrives signed with a foreign key")
    public void aMessageWithForeignToken(String username) {
        send(body(username, "ADD", "2026-11-10", 60), ServiceTokens.bearer(ServiceTokens.OTHER_SECRET, "gym-crm",
                "trainer-workload-service", Duration.ofMinutes(5)), username);
    }

    @When("a workload message arrives with the body {}")
    public void aMessageWithTheBody(String body) {
        send(body, ServiceTokens.valid(), "Nodira.Karimova");
    }

    @Then("the workload of {string} has these months:")
    public void theWorkloadHasTheseMonths(String username, DataTable months) {
        List<Map<String, String>> expected = months.asMaps();
        await().atMost(PROCESSING_TIMEOUT).untilAsserted(() -> {
            TrainerWorkload workload = repository.findByUsername(username).orElseThrow();
            for (Map<String, String> row : expected) {
                assertEquals(Long.parseLong(row.get("minutes")), workload.getMonthlyDuration(
                        Integer.parseInt(row.get("year")), Integer.parseInt(row.get("month"))), row::toString);
            }
            long storedMonths = workload.getYears().stream().mapToLong(year -> year.getMonths().size()).sum();
            assertEquals(expected.size(), storedMonths);
        });
    }

    @Then("the workload of {string} has no months")
    public void theWorkloadHasNoMonths(String username) {
        await().atMost(PROCESSING_TIMEOUT).untilAsserted(() ->
                assertTrue(repository.findByUsername(username).orElseThrow().getYears().isEmpty()));
    }

    @Then("{string} is stored as {string} {string} with status active")
    public void isStoredAs(String username, String firstName, String lastName) {
        TrainerWorkload workload = repository.findByUsername(username).orElseThrow();
        assertEquals(firstName, workload.getFirstName());
        assertEquals(lastName, workload.getLastName());
        assertTrue(workload.isActive());
    }

    @Then("after the message is processed the trainer {string} still has no workload")
    public void afterProcessingStillNoWorkload(String username) {
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(2))
                .until(() -> repository.findByUsername(username).isEmpty());
    }

    @Then("the message is moved to the dead letter queue with reason {string}")
    public void movedToDeadLetterQueueWithReason(String reason) throws JMSException {
        assertEquals(reason, deadLetterReason());
    }

    @Then("the message is moved to the dead letter queue with a reason starting with {string}")
    public void movedToDeadLetterQueueWithReasonStartingWith(String prefix) throws JMSException {
        String reason = deadLetterReason();
        assertTrue(reason.startsWith(prefix), () -> "Reason: " + reason);
    }

    private String deadLetterReason() throws JMSException {
        jms.setReceiveTimeout(PROCESSING_TIMEOUT.toMillis());
        TextMessage deadLetter = (TextMessage) jms.receive(DEAD_LETTER_QUEUE);
        assertNotNull(deadLetter, "Nothing arrived in the dead letter queue");
        return deadLetter.getStringProperty(WorkloadMessageHeaders.DEAD_LETTER_REASON);
    }

    private long minutesIn(String username, String date) {
        java.time.LocalDate day = java.time.LocalDate.parse(date);
        return repository.findByUsername(username)
                .map(workload -> workload.getMonthlyDuration(day.getYear(), day.getMonthValue()))
                .orElse(-1L);
    }

    private void send(String body, String authorization, String group) {
        jms.convertAndSend(QUEUE, body, message -> {
            if (authorization != null) {
                message.setStringProperty(WorkloadMessageHeaders.AUTHORIZATION, authorization);
            }
            message.setStringProperty(WorkloadMessageHeaders.TRANSACTION_ID, "bdd-" + UUID.randomUUID());
            message.setStringProperty("JMSXGroupID", group);
            message.setStringProperty(WorkloadMessageHeaders.TYPE_ID, "trainerWorkload");
            return message;
        });
    }

    private String body(String username, String action, String date, int minutes) {
        ScenarioState.TrainerName name = state.trainer(username);
        return "{\"trainerUsername\":\"" + username + "\",\"trainerFirstName\":\"" + name.firstName()
                + "\",\"trainerLastName\":\"" + name.lastName() + "\",\"isActive\":true,\"trainingDate\":\"" + date
                + "\",\"trainingDuration\":" + minutes + ",\"actionType\":\"" + action + "\"}";
    }
}
