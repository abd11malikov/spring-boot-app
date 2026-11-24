package com.epam.gym.workload;

/**
 * Tells the Trainer Workload service about trainings added or deleted here.
 */
public interface TrainerWorkloadPublisher {

    /**
     * Queues the message. It is delivered only if the caller's database transaction commits.
     *
     * @throws com.epam.gym.exception.WorkloadServiceUnavailableException if the message broker cannot be reached
     *                                                                    or its circuit breaker is open
     */
    void publish(TrainerWorkloadMessage message);
}
