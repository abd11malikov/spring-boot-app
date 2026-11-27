package com.epam.trainerworkload.messaging;

/**
 * JMS properties of the trainer workload messages, as set by Gym CRM, and the ones added to dead letters.
 */
public final class WorkloadMessageHeaders {

    /** {@code Bearer <service JWT>} */
    public static final String AUTHORIZATION = "authorization";
    public static final String TRANSACTION_ID = "transactionId";
    /** Set by ActiveMQ: 1 on the first delivery, 2 on the first redelivery, ... */
    public static final String DELIVERY_COUNT = "JMSXDeliveryCount";
    /** Logical type of the JSON body. */
    public static final String TYPE_ID = "_type";

    /** Why a message was dead-lettered. */
    public static final String DEAD_LETTER_REASON = "deadLetterReason";
    /** The queue the dead letter originally came from. */
    public static final String ORIGINAL_QUEUE = "originalQueue";

    private WorkloadMessageHeaders() {
    }
}
