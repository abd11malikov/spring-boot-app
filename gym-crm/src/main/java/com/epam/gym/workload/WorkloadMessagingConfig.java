package com.epam.gym.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/**
 * Wiring of the Trainer Workload integration over ActiveMQ. The connection factory and {@link JmsTemplate} are
 * Spring Boot's, configured per environment through {@code spring.activemq.*} and {@code spring.jms.*}.
 */
@Configuration
public class WorkloadMessagingConfig {

    /**
     * JSON text messages. The body's type travels as a logical name, not a Java class name.
     */
    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName(WorkloadMessageHeaders.TYPE_ID);
        converter.setTypeIdMappings(Map.of(WorkloadMessageHeaders.TRAINER_WORKLOAD_TYPE, TrainerWorkloadMessage.class));
        return converter;
    }

    @Bean
    @ConditionalOnProperty(prefix = "gym.workload", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ServiceTokenProvider serviceTokenProvider(@Value("${gym.workload.jwt.secret}") String secret,
                                                     @Value("${spring.application.name}") String issuer,
                                                     @Value("${gym.workload.jwt.audience}") String audience,
                                                     @Value("${gym.workload.jwt.expiration}") Duration validity,
                                                     Clock clock) {
        return new ServiceTokenProvider(secret, issuer, audience, validity, clock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "gym.workload", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TrainerWorkloadPublisher jmsTrainerWorkloadPublisher(JmsTemplate jmsTemplate,
                                                                @Value("${gym.workload.queue}") String queue,
                                                                ServiceTokenProvider tokenProvider) {
        return new JmsTrainerWorkloadPublisher(jmsTemplate, queue, tokenProvider);
    }

    @Bean
    @ConditionalOnProperty(prefix = "gym.workload", name = "enabled", havingValue = "false")
    public TrainerWorkloadPublisher disabledTrainerWorkloadPublisher() {
        return new DisabledTrainerWorkloadPublisher();
    }
}
