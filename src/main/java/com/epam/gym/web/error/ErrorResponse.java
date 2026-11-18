package com.epam.gym.web.error;

import com.epam.gym.web.logging.TransactionIdFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(Instant timestamp,
                            int status,
                            String error,
                            String message,
                            String path,
                            String transactionId,
                            List<FieldViolation> violations) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, List.of());
    }

    public static ErrorResponse of(HttpStatus status, String message, String path, List<FieldViolation> violations) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path,
                MDC.get(TransactionIdFilter.MDC_KEY), violations);
    }

    public record FieldViolation(String field, String message) {
    }
}
