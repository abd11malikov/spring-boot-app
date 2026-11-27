package com.epam.trainerworkload.messaging;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import java.time.Duration;

/**
 * ActiveMQ consumer setup on top of Spring Boot's auto-configuration ({@code spring.activemq.*} per profile).
 */
@Configuration
public class MessagingConfig {
    private static final Logger log = LoggerFactory.getLogger(MessagingConfig.class);

    /**
     * Redelivery of rolled-back messages with exponential backoff. The client allows one more redelivery than
     * the listener's own limit, so the listener dead-letters a message itself (with a reason) before ActiveMQ's
     * fallback, the broker's {@code ActiveMQ.DLQ}, is ever needed.
     */
    @Bean
    public ActiveMQConnectionFactoryCustomizer redeliveryPolicyCustomizer(
            @Value("${workload.messaging.max-delivery-attempts}") int maxDeliveryAttempts,
            @Value("${workload.messaging.redelivery-delay}") Duration initialDelay) {
        return connectionFactory -> {
            RedeliveryPolicy policy = connectionFactory.getRedeliveryPolicy();
            policy.setMaximumRedeliveries(maxDeliveryAttempts);
            policy.setInitialRedeliveryDelay(initialDelay.toMillis());
            policy.setRedeliveryDelay(initialDelay.toMillis());
            policy.setUseExponentialBackOff(true);
            policy.setBackOffMultiplier(2);
        };
    }

    /**
     * Boot's listener container settings, plus logging of failures that end in a redelivery.
     */
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            DefaultJmsListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setSessionTransacted(true);
        factory.setErrorHandler(error -> log.debug("Listener failed, message rolled back for redelivery: {}",
                error.toString()));
        return factory;
    }
}
