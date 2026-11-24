package com.epam.trainerworkload.repository;

import com.epam.trainerworkload.model.TrainerWorkload;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTrainerWorkloadRepositoryTest {

    private final InMemoryTrainerWorkloadRepository repository = new InMemoryTrainerWorkloadRepository();

    @Test
    void update_unknownTrainer_receivesNullAndStoresResult() {
        // when
        repository.update("Nodira.Karimova", current -> {
            assertEquals(null, current);
            return workload();
        });

        // then
        assertTrue(repository.findByUsername("Nodira.Karimova").isPresent());
    }

    @Test
    void update_returningNull_storesNothing() {
        // when
        repository.update("Nobody.Here", current -> null);

        // then
        assertTrue(repository.findByUsername("Nobody.Here").isEmpty());
    }

    @Test
    void findByUsername_returnsCopy() {
        // given
        repository.update("Nodira.Karimova", current -> workload());

        // when
        repository.findByUsername("Nodira.Karimova").orElseThrow().addTraining(LocalDate.of(2026, 11, 1), 999);

        // then
        assertEquals(0, repository.findByUsername("Nodira.Karimova").orElseThrow().getMonthlyDuration(2026, 11));
    }

    @Test
    void failedUpdate_leavesStoredWorkloadUntouched() {
        // given
        repository.update("Nodira.Karimova", current -> workload());

        // when
        try {
            repository.update("Nodira.Karimova", current -> {
                current.addTraining(LocalDate.of(2026, 11, 1), 60);
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException expected) {
            // the change failed half-way
        }

        // then
        assertEquals(0, repository.findByUsername("Nodira.Karimova").orElseThrow().getMonthlyDuration(2026, 11));
    }

    @Test
    void concurrentUpdatesOfOneTrainer_areAllApplied() throws InterruptedException {
        // given
        int updates = 200;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch done = new CountDownLatch(updates);

        // when
        for (int i = 0; i < updates; i++) {
            executor.submit(() -> {
                repository.update("Nodira.Karimova", current -> {
                    TrainerWorkload workload = current != null ? current : workload();
                    workload.addTraining(LocalDate.of(2026, 11, 1), 1);
                    return workload;
                });
                done.countDown();
            });
        }
        assertTrue(done.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // then
        assertEquals(updates, repository.findByUsername("Nodira.Karimova").orElseThrow().getMonthlyDuration(2026, 11));
    }

    private TrainerWorkload workload() {
        return new TrainerWorkload("Nodira.Karimova", "Nodira", "Karimova", true);
    }
}
