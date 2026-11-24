package com.epam.trainerworkload.service;

import com.epam.trainerworkload.model.TrainerWorkload;

public interface TrainerWorkloadService {

    /**
     * Adds the training's duration to its month ({@code ADD}) or takes it away ({@code DELETE}).
     */
    void apply(TrainerWorkloadUpdate update);

    /**
     * @throws java.util.NoSuchElementException for a trainer without any workload
     */
    TrainerWorkload getWorkload(String username);

    /**
     * @return 0 minutes for a month without trainings
     * @throws java.util.NoSuchElementException for a trainer without any workload
     */
    MonthlyWorkload getMonthlyWorkload(String username, int year, int month);
}
