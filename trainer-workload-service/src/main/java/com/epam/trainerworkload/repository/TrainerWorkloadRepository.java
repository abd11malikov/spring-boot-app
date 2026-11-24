package com.epam.trainerworkload.repository;

import com.epam.trainerworkload.model.TrainerWorkload;

import java.util.Optional;
import java.util.function.UnaryOperator;

public interface TrainerWorkloadRepository {

    /**
     * @return a copy: changing it does not change the stored workload
     */
    Optional<TrainerWorkload> findByUsername(String username);

    /**
     * Atomically replaces the workload of {@code username} with {@code change} applied to it.
     * The function receives {@code null} for an unknown trainer and may return {@code null} to store nothing.
     *
     * @return a copy of the stored result
     */
    Optional<TrainerWorkload> update(String username, UnaryOperator<TrainerWorkload> change);
}
