package com.epam.gym.monitoring.health;

import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.model.TrainingType;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrainingTypesHealthIndicator implements HealthIndicator {

    private final TrainingTypeDao trainingTypeDao;

    public TrainingTypesHealthIndicator(TrainingTypeDao trainingTypeDao) {
        this.trainingTypeDao = trainingTypeDao;
    }

    @Override
    public Health health() {
        try {
            List<String> names = trainingTypeDao.findAll().stream()
                    .map(TrainingType::getTrainingTypeName)
                    .toList();
            if (names.isEmpty()) {
                return Health.down()
                        .withDetail("reason", "No training types loaded; trainers cannot be registered")
                        .build();
            }
            return Health.up()
                    .withDetail("count", names.size())
                    .withDetail("types", names)
                    .build();
        } catch (RuntimeException e) {
            return Health.down(e).build();
        }
    }
}
