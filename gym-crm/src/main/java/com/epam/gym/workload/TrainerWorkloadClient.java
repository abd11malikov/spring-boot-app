package com.epam.gym.workload;

/**
 * Keeps the Trainer Workload service in step with the trainings stored here.
 */
public interface TrainerWorkloadClient {

    /**
     * @throws com.epam.gym.exception.WorkloadServiceUnavailableException if the service cannot be reached in
     *                                                                    time or its circuit breaker is open
     */
    void updateWorkload(TrainerWorkloadRequest request);
}
