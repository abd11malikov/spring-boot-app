package com.epam.gym.monitoring.metrics;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ActiveUsersMetrics {

    static final String ACTIVE_USERS = "gym.users.active";

    public ActiveUsersMetrics(MeterRegistry registry, TraineeDao traineeDao, TrainerDao trainerDao) {
        Gauge.builder(ACTIVE_USERS, traineeDao, TraineeDao::countActive)
                .description("Active trainee profiles")
                .tag("role", "trainee")
                .register(registry);
        Gauge.builder(ACTIVE_USERS, trainerDao, TrainerDao::countActive)
                .description("Active trainer profiles")
                .tag("role", "trainer")
                .register(registry);
    }
}
