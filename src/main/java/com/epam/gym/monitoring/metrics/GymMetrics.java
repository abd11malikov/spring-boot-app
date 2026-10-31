package com.epam.gym.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class GymMetrics {

    static final String REGISTRATIONS = "gym.registrations";
    static final String AUTHENTICATIONS = "gym.authentications";
    static final String TRAININGS_ADDED = "gym.trainings.added";
    static final String TRAINING_SEARCH = "gym.trainings.search";

    private final Counter traineeRegistrations;
    private final Counter trainerRegistrations;
    private final Counter successfulAuthentications;
    private final Counter failedAuthentications;
    private final Counter trainingsAdded;
    private final Timer traineeTrainingSearch;
    private final Timer trainerTrainingSearch;

    public GymMetrics(MeterRegistry registry) {
        traineeRegistrations = registrationCounter(registry, "trainee");
        trainerRegistrations = registrationCounter(registry, "trainer");
        successfulAuthentications = authenticationCounter(registry, "success");
        failedAuthentications = authenticationCounter(registry, "failure");
        trainingsAdded = Counter.builder(TRAININGS_ADDED)
                .description("Trainings added")
                .register(registry);
        traineeTrainingSearch = searchTimer(registry, "trainee");
        trainerTrainingSearch = searchTimer(registry, "trainer");
    }

    public void traineeRegistered() {
        traineeRegistrations.increment();
    }

    public void trainerRegistered() {
        trainerRegistrations.increment();
    }

    public void authenticationSucceeded() {
        successfulAuthentications.increment();
    }

    public void authenticationFailed() {
        failedAuthentications.increment();
    }

    public void trainingAdded() {
        trainingsAdded.increment();
    }

    public <T> T timeTraineeTrainingSearch(Supplier<T> search) {
        return traineeTrainingSearch.record(search);
    }

    public <T> T timeTrainerTrainingSearch(Supplier<T> search) {
        return trainerTrainingSearch.record(search);
    }

    private Counter registrationCounter(MeterRegistry registry, String role) {
        return Counter.builder(REGISTRATIONS)
                .description("Successful profile registrations")
                .tag("role", role)
                .register(registry);
    }

    private Counter authenticationCounter(MeterRegistry registry, String result) {
        return Counter.builder(AUTHENTICATIONS)
                .description("Username and password checks")
                .tag("result", result)
                .register(registry);
    }

    private Timer searchTimer(MeterRegistry registry, String side) {
        return Timer.builder(TRAINING_SEARCH)
                .description("Time spent searching trainings by criteria")
                .tag("side", side)
                .register(registry);
    }
}
