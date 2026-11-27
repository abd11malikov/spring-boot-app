package com.epam.trainerworkload.service;

import com.epam.trainerworkload.model.TrainerWorkload;

import java.util.List;

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

    /**
     * @return the trainers with exactly this first and last name (several trainers may share a name)
     */
    List<TrainerWorkload> findByName(String firstName, String lastName);
}
