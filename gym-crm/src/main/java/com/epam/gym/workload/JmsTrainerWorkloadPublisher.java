package com.epam.gym.workload;

import com.epam.gym.exception.WorkloadServiceUnavailableException;
import com.epam.gym.web.logging.TransactionIdFilter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jms.core.JmsTemplate;

/**
 * Sends trainer workload messages to the ActiveMQ queue through Spring's {@link JmsTemplate}.
 * <p>
 * The template uses a transacted session, which Spring commits only after the surrounding database
 * transaction commits: a rolled-back operation sends nothing. Each message carries, as JMS properties:
 * <ul>
 *     <li>{@value WorkloadMessageHeaders#AUTHORIZATION}: the service JWT, checked by the consumer;</li>
 *     <li>{@value WorkloadMessageHeaders#TRANSACTION_ID}: the current transaction id, for cross-service logs;</li>
 *     <li>{@code JMSXGroupID}: the trainer's username, so ActiveMQ hands all of a trainer's messages to the same
 *     consumer in order, however many consumers run.</li>
 * </ul>
 * If the broker can't be reached, or the circuit breaker is open, {@link #unavailable} reports
 * {@link WorkloadServiceUnavailableException} and the caller's transaction is rolled back.
 */
public class JmsTrainerWorkloadPublisher implements TrainerWorkloadPublisher {
    private static final Logger log = LoggerFactory.getLogger(JmsTrainerWorkloadPublisher.class);

    static final String CIRCUIT_BREAKER = "trainerWorkload";

    private final JmsTemplate jmsTemplate;
    private final String queue;
    private final ServiceTokenProvider tokenProvider;

    public JmsTrainerWorkloadPublisher(JmsTemplate jmsTemplate, String queue, ServiceTokenProvider tokenProvider) {
        this.jmsTemplate = jmsTemplate;
        this.queue = queue;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "unavailable")
    public void publish(TrainerWorkloadMessage message) {
        String transactionId = MDC.get(TransactionIdFilter.MDC_KEY);
        jmsTemplate.convertAndSend(queue, message, jmsMessage -> {
            jmsMessage.setStringProperty(WorkloadMessageHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getToken());
            jmsMessage.setStringProperty(WorkloadMessageHeaders.GROUP_ID, message.trainerUsername());
            if (transactionId != null) {
                jmsMessage.setStringProperty(WorkloadMessageHeaders.TRANSACTION_ID, transactionId);
            }
            return jmsMessage;
        });
        log.info("Queued {} of {} minutes on {} for trainer {} to {}", message.actionType(), message.trainingDuration(),
                message.trainingDate(), message.trainerUsername(), queue);
    }

    @SuppressWarnings("unused") // circuit breaker fallback, called reflectively
    private void unavailable(TrainerWorkloadMessage message, Throwable cause) {
        if (cause instanceof CallNotPermittedException) {
            log.warn("Workload circuit breaker is open: {} for trainer {} not queued", message.actionType(),
                    message.trainerUsername());
        } else {
            log.error("Could not queue {} for trainer {}: {}", message.actionType(), message.trainerUsername(),
                    cause.toString());
        }
        throw new WorkloadServiceUnavailableException(cause);
    }
}
