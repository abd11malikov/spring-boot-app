package com.epam.gym.component;

import com.epam.gym.workload.WorkloadMessageHeaders;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what Gym CRM put on the trainer workload queue: in these component tests the queue stands in for the
 * Trainer Workload service.
 */
public class WorkloadQueueSteps {

    private final WorkloadQueue queue;
    private final ScenarioState state;
    private final String serviceSecret;
    private TextMessage lastMessage;

    public WorkloadQueueSteps(WorkloadQueue queue, ScenarioState state,
                              @Value("${gym.workload.jwt.secret}") String serviceSecret) {
        this.queue = queue;
        this.state = state;
        this.serviceSecret = serviceSecret;
    }

    @Before
    public void emptyTheQueue() {
        queue.drain();
    }

    @Then("an {string} workload message for {string} with {int} minutes on {string} is queued")
    public void aWorkloadMessageIsQueued(String action, String trainer, int minutes, String date) throws JMSException {
        lastMessage = receive();
        assertNotNull(lastMessage, "No workload message was queued");
        assertMessage(lastMessage.getText(), state.account(trainer).username(), action, date, minutes);
    }

    @Then("the message carries a valid service token and transaction id {string}")
    public void theMessageCarriesTokenAndTransactionId(String transactionId) throws JMSException {
        assertEquals(transactionId, lastMessage.getStringProperty(WorkloadMessageHeaders.TRANSACTION_ID));
        String authorization = lastMessage.getStringProperty(WorkloadMessageHeaders.AUTHORIZATION);
        assertTrue(authorization.startsWith("Bearer "));
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(serviceSecret)))
                .build()
                .parseSignedClaims(authorization.substring("Bearer ".length()))
                .getPayload();
        assertEquals("gym-crm", claims.getIssuer());
        assertTrue(claims.getAudience().contains("trainer-workload-service"));
    }

    @Then("these workload messages for {string} are queued:")
    public void theseWorkloadMessagesAreQueued(String trainer, DataTable expected) throws JMSException {
        List<String> bodies = new ArrayList<>();
        TextMessage message;
        while ((message = receive()) != null) {
            bodies.add(message.getText());
        }
        List<Map<String, String>> rows = expected.asMaps();
        assertEquals(rows.size(), bodies.size(), () -> "Queued: " + bodies);
        for (Map<String, String> row : rows) {
            assertTrue(bodies.stream().anyMatch(body -> matches(body, state.account(trainer).username(),
                            row.get("action"), row.get("date"), Integer.parseInt(row.get("minutes")))),
                    () -> "Missing " + row + " in " + bodies);
        }
    }

    @Then("no workload message is queued")
    public void noWorkloadMessageIsQueued() {
        assertNull(queue.next(500), "Unexpected workload message");
    }

    private TextMessage receive() {
        return queue.next(3000);
    }

    private static void assertMessage(String body, String trainer, String action, String date, int minutes) {
        assertTrue(matches(body, trainer, action, date, minutes), () -> "Message: " + body);
    }

    private static boolean matches(String body, String trainer, String action, String date, int minutes) {
        return trainer.equals(JsonPath.read(body, "$.trainerUsername"))
                && action.equals(JsonPath.read(body, "$.actionType"))
                && date.equals(JsonPath.read(body, "$.trainingDate"))
                && minutes == (int) JsonPath.read(body, "$.trainingDuration");
    }
}
