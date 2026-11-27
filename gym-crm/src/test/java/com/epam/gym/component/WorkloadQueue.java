package com.epam.gym.component;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.TextMessage;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * The trainer workload queue, read the way the Trainer Workload service would. In the component tests it stands
 * in for that service.
 */
@Component
public class WorkloadQueue {

    private static final String QUEUE = "trainer.workload";

    private final JmsTemplate jms;

    public WorkloadQueue(ConnectionFactory connectionFactory) {
        this.jms = new JmsTemplate(connectionFactory);
    }

    /**
     * @return the next message, or {@code null} if none arrives within the timeout
     */
    public TextMessage next(long timeoutMillis) {
        jms.setReceiveTimeout(timeoutMillis);
        return (TextMessage) jms.receive(QUEUE);
    }

    /**
     * Removes the messages already queued, e.g. those caused by a scenario's setup steps.
     */
    public void drain() {
        while (next(200) != null) {
            // discarded
        }
    }
}
