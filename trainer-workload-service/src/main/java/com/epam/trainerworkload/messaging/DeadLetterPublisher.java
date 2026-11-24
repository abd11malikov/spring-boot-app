package com.epam.trainerworkload.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Moves a message that can't be processed to the dead letter queue, with the reason and its origin as
 * properties. The service token is deliberately not copied: dead letters are read by people.
 * <p>
 * Called inside the listener's transacted session, so moving the message and acknowledging the original
 * commit together.
 */
@Component
public class DeadLetterPublisher {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterPublisher.class);

    private final JmsTemplate jmsTemplate;
    private final String deadLetterQueue;
    private final String queue;

    public DeadLetterPublisher(JmsTemplate jmsTemplate,
                               @Value("${workload.messaging.dead-letter-queue}") String deadLetterQueue,
                               @Value("${workload.messaging.queue}") String queue) {
        this.jmsTemplate = jmsTemplate;
        this.deadLetterQueue = deadLetterQueue;
        this.queue = queue;
    }

    public void deadLetter(String payload, String transactionId, String reason) {
        jmsTemplate.convertAndSend(deadLetterQueue, payload == null ? "" : payload, message -> {
            message.setStringProperty(WorkloadMessageHeaders.DEAD_LETTER_REASON, reason);
            message.setStringProperty(WorkloadMessageHeaders.ORIGINAL_QUEUE, queue);
            if (transactionId != null) {
                message.setStringProperty(WorkloadMessageHeaders.TRANSACTION_ID, transactionId);
            }
            return message;
        });
        log.warn("Moved message to {}: {}", deadLetterQueue, reason);
    }
}
