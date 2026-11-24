package com.epam.gym.monitoring.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GymMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final GymMetrics metrics = new GymMetrics(registry);

    @Test
    void registrations_areCountedPerRole() {
        // given
        int trainees = 2;

        // when
        for (int i = 0; i < trainees; i++) {
            metrics.traineeRegistered();
        }
        metrics.trainerRegistered();

        // then
        assertEquals(2.0, registry.get(GymMetrics.REGISTRATIONS).tag("role", "trainee").counter().count());
        assertEquals(1.0, registry.get(GymMetrics.REGISTRATIONS).tag("role", "trainer").counter().count());
    }

    @Test
    void authentications_areCountedPerResult() {
        // given
        metrics.authenticationSucceeded();

        // when
        metrics.authenticationFailed();
        metrics.authenticationFailed();

        // then
        assertEquals(1.0, registry.get(GymMetrics.AUTHENTICATIONS).tag("result", "success").counter().count());
        assertEquals(2.0, registry.get(GymMetrics.AUTHENTICATIONS).tag("result", "failure").counter().count());
    }

    @Test
    void trainingAdded_incrementsCounter() {
        // given
        double before = registry.get(GymMetrics.TRAININGS_ADDED).counter().count();

        // when
        metrics.trainingAdded();

        // then
        assertEquals(before + 1, registry.get(GymMetrics.TRAININGS_ADDED).counter().count());
    }

    @Test
    void timeTraineeTrainingSearch_recordsCallAndReturnsResult() {
        // given
        String expected = "result";

        // when
        String result = metrics.timeTraineeTrainingSearch(() -> expected);

        // then
        assertEquals(expected, result);
        assertEquals(1, registry.get(GymMetrics.TRAINING_SEARCH).tag("side", "trainee").timer().count());
        assertEquals(0, registry.get(GymMetrics.TRAINING_SEARCH).tag("side", "trainer").timer().count());
    }

    @Test
    void allMeters_areRegisteredUpFront() {
        // given
        GymMetrics freshMetrics = new GymMetrics(new SimpleMeterRegistry());

        // when
        int counters = registry.find(GymMetrics.REGISTRATIONS).counters().size();

        // then
        assertEquals(2, counters, "Both role counters exist before any registration: " + freshMetrics);
    }
}
