package com.epam.gym.monitoring.metrics;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveUsersMetricsTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @Test
    void gauges_readActiveCountsFromDatabaseOnEachScrape() {
        // given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new ActiveUsersMetrics(registry, traineeDao, trainerDao);
        when(traineeDao.countActive()).thenReturn(5L, 6L);
        when(trainerDao.countActive()).thenReturn(2L);

        // when
        double firstRead = registry.get(ActiveUsersMetrics.ACTIVE_USERS).tag("role", "trainee").gauge().value();
        double secondRead = registry.get(ActiveUsersMetrics.ACTIVE_USERS).tag("role", "trainee").gauge().value();
        double trainers = registry.get(ActiveUsersMetrics.ACTIVE_USERS).tag("role", "trainer").gauge().value();

        // then
        assertEquals(5.0, firstRead);
        assertEquals(6.0, secondRead);
        assertEquals(2.0, trainers);
    }
}
