package com.epam.trainerworkload.messaging;

/**
 * A message that can never be processed (bad JSON, missing or invalid fields, missing or invalid token):
 * it goes straight to the dead letter queue instead of being retried.
 */
public class InvalidWorkloadMessageException extends RuntimeException {

    public InvalidWorkloadMessageException(String message) {
        super(message);
    }
}
