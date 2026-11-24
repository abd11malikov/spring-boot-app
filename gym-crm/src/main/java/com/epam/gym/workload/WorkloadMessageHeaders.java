package com.epam.gym.workload;

/**
 * JMS properties of the trainer workload messages. JMS property names must be valid Java identifiers,
 * so they differ from the HTTP header names.
 */
public final class WorkloadMessageHeaders {

    /** {@code Bearer <service JWT>} */
    public static final String AUTHORIZATION = "authorization";
    public static final String TRANSACTION_ID = "transactionId";
    /** ActiveMQ message group: one consumer at a time handles a group, in order. */
    public static final String GROUP_ID = "JMSXGroupID";
    /** Logical type of the JSON body, so neither side depends on the other's class names. */
    public static final String TYPE_ID = "_type";
    public static final String TRAINER_WORKLOAD_TYPE = "trainerWorkload";

    private WorkloadMessageHeaders() {
    }
}
