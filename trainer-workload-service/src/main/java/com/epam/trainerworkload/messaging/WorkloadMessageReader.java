package com.epam.trainerworkload.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns a raw message into a {@link TrainerWorkloadMessage}: checks the service token, reads the JSON body and
 * validates the required fields. Anything wrong is an {@link InvalidWorkloadMessageException} naming the problem.
 * <p>
 * The body is read here, not by the listener container's converter, so that a malformed body becomes a dead
 * letter with a reason instead of failing in the container and being retried pointlessly.
 */
@Component
public class WorkloadMessageReader {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final JwtDecoder jwtDecoder;

    public WorkloadMessageReader(ObjectMapper objectMapper, Validator validator, JwtDecoder jwtDecoder) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.jwtDecoder = jwtDecoder;
    }

    public TrainerWorkloadMessage read(String payload, String authorization) {
        verifyToken(authorization);
        TrainerWorkloadMessage message = parse(payload);
        validate(message);
        return message;
    }

    private void verifyToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new InvalidWorkloadMessageException("Missing service token");
        }
        try {
            jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()));
        } catch (JwtException e) {
            throw new InvalidWorkloadMessageException("Invalid service token");
        }
    }

    private TrainerWorkloadMessage parse(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new InvalidWorkloadMessageException("Empty message body");
        }
        try {
            return objectMapper.readValue(payload, TrainerWorkloadMessage.class);
        } catch (JsonProcessingException e) {
            throw new InvalidWorkloadMessageException("Malformed JSON body: " + e.getOriginalMessage());
        }
    }

    private void validate(TrainerWorkloadMessage message) {
        Set<ConstraintViolation<TrainerWorkloadMessage>> violations = validator.validate(message);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .collect(Collectors.joining(", "));
            throw new InvalidWorkloadMessageException("Invalid fields: " + details);
        }
    }
}
