package com.epam.gym.exception;

/**
 * The trainer workload update could not be queued (message broker unreachable or too slow, or the circuit
 * breaker is open). The operation is rolled back and can be retried later.
 */
public class WorkloadServiceUnavailableException extends RuntimeException {

    public WorkloadServiceUnavailableException(Throwable cause) {
        super("Trainer workload updates are temporarily unavailable, please try again later", cause);
    }
}
