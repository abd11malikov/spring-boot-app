package com.epam.trainerworkload.messaging;

import com.epam.trainerworkload.service.TrainerWorkloadService;
import com.epam.trainerworkload.web.logging.TransactionIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes the trainer workload queue.
 * <ul>
 *     <li>A valid message updates the workload and is acknowledged.</li>
 *     <li>An invalid one (bad token, malformed JSON, missing fields) goes straight to the dead letter queue:
 *     retrying can't fix it.</li>
 *     <li>A failure while processing a valid message is rethrown, so the transacted session rolls back and
 *     ActiveMQ redelivers it with a growing delay. On the last allowed attempt it goes to the dead letter queue
 *     instead.</li>
 * </ul>
 * The listener container runs several of these consumers concurrently ({@code workload.messaging.concurrency});
 * Gym CRM groups messages by trainer, so one trainer's messages are still handled one at a time and in order.
 */
@Component
public class TrainerWorkloadListener {
    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadListener.class);

    private final WorkloadMessageReader reader;
    private final TrainerWorkloadService service;
    private final DeadLetterPublisher deadLetterPublisher;
    private final int maxDeliveryAttempts;

    public TrainerWorkloadListener(WorkloadMessageReader reader,
                                   TrainerWorkloadService service,
                                   DeadLetterPublisher deadLetterPublisher,
                                   @Value("${workload.messaging.max-delivery-attempts}") int maxDeliveryAttempts) {
        this.reader = reader;
        this.service = service;
        this.deadLetterPublisher = deadLetterPublisher;
        this.maxDeliveryAttempts = maxDeliveryAttempts;
    }

    @JmsListener(destination = "${workload.messaging.queue}", concurrency = "${workload.messaging.concurrency}")
    public void onMessage(@Payload String payload,
                          @Header(name = WorkloadMessageHeaders.AUTHORIZATION, required = false) String authorization,
                          @Header(name = WorkloadMessageHeaders.TRANSACTION_ID, required = false) String transactionId,
                          @Header(name = WorkloadMessageHeaders.DELIVERY_COUNT, required = false) Integer deliveryCount) {
        MDC.put(TransactionIdFilter.MDC_KEY, transactionId != null ? transactionId : UUID.randomUUID().toString());
        try {
            handle(payload, authorization, transactionId, deliveryCount == null ? 1 : deliveryCount);
        } finally {
            MDC.remove(TransactionIdFilter.MDC_KEY);
        }
    }

    private void handle(String payload, String authorization, String transactionId, int attempt) {
        TrainerWorkloadMessage message;
        try {
            message = reader.read(payload, authorization);
        } catch (InvalidWorkloadMessageException e) {
            log.warn("Rejected invalid workload message: {}", e.getMessage());
            deadLetterPublisher.deadLetter(payload, transactionId, e.getMessage());
            return;
        }

        log.debug("Received {} for trainer {} (attempt {})", message.actionType(), message.trainerUsername(), attempt);
        try {
            service.apply(message.toUpdate());
        } catch (RuntimeException e) {
            if (attempt >= maxDeliveryAttempts) {
                log.error("Giving up on {} for trainer {} after {} attempts", message.actionType(),
                        message.trainerUsername(), attempt, e);
                deadLetterPublisher.deadLetter(payload, transactionId,
                        "Processing failed after " + attempt + " attempts: " + e.getMessage());
                return;
            }
            log.warn("Processing {} for trainer {} failed (attempt {} of {}), it will be redelivered: {}",
                    message.actionType(), message.trainerUsername(), attempt, maxDeliveryAttempts, e.toString());
            throw e;
        }
    }
}
