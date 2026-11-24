package com.epam.trainerworkload.repository;

import com.epam.trainerworkload.model.TrainerWorkload;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * In-memory database of trainer workloads.
 * <p>
 * A stored workload is never modified: an update works on a copy and {@link ConcurrentHashMap#compute}
 * swaps it in atomically, so concurrent updates of one trainer are serialized and readers never see a
 * half-applied change.
 */
@Repository
public class InMemoryTrainerWorkloadRepository implements TrainerWorkloadRepository {

    private final Map<String, TrainerWorkload> workloads = new ConcurrentHashMap<>();

    @Override
    public Optional<TrainerWorkload> findByUsername(String username) {
        return Optional.ofNullable(workloads.get(username)).map(TrainerWorkload::copy);
    }

    @Override
    public Optional<TrainerWorkload> update(String username, UnaryOperator<TrainerWorkload> change) {
        TrainerWorkload stored = workloads.compute(username,
                (key, current) -> change.apply(current == null ? null : current.copy()));
        return Optional.ofNullable(stored).map(TrainerWorkload::copy);
    }
}
