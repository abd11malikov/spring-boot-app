package com.epam.gym.monitoring.health;

import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.model.TrainingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypesHealthIndicatorTest {

    @Mock
    private TrainingTypeDao trainingTypeDao;

    @Test
    void health_typesLoaded_isUpWithCountAndNames() {
        // given
        when(trainingTypeDao.findAll()).thenReturn(List.of(new TrainingType("Cardio"), new TrainingType("Yoga")));
        TrainingTypesHealthIndicator indicator = new TrainingTypesHealthIndicator(trainingTypeDao);

        // when
        Health health = indicator.health();

        // then
        assertEquals(Status.UP, health.getStatus());
        assertEquals(2, health.getDetails().get("count"));
        assertEquals(List.of("Cardio", "Yoga"), health.getDetails().get("types"));
    }

    @Test
    void health_noTypes_isDown() {
        // given
        when(trainingTypeDao.findAll()).thenReturn(List.of());
        TrainingTypesHealthIndicator indicator = new TrainingTypesHealthIndicator(trainingTypeDao);

        // when
        Health health = indicator.health();

        // then
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("No training types loaded; trainers cannot be registered", health.getDetails().get("reason"));
    }

    @Test
    void health_databaseError_isDownWithError() {
        // given
        when(trainingTypeDao.findAll()).thenThrow(new IllegalStateException("connection refused"));
        TrainingTypesHealthIndicator indicator = new TrainingTypesHealthIndicator(trainingTypeDao);

        // when
        Health health = indicator.health();

        // then
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("java.lang.IllegalStateException: connection refused", health.getDetails().get("error"));
    }
}
