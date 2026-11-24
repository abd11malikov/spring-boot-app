package com.epam.gym.exception;

/**
 * The Trainer Workload service could not be updated (unreachable, too slow, failing, or its circuit breaker
 * is open). The operation is rolled back and can be retried later.
 */
public class WorkloadServiceUnavailableException extends RuntimeException {

    public WorkloadServiceUnavailableException(Throwable cause) {
        super("Trainer workload service is temporarily unavailable, please try again later", cause);
    }
}
