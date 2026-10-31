package com.epam.gym.monitoring.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryHealthIndicatorTest {

    private static final long MB = 1024 * 1024;

    @Mock
    private MemoryMXBean memoryBean;

    @Test
    void health_usageBelowThreshold_isUp() {
        // given
        when(memoryBean.getHeapMemoryUsage()).thenReturn(new MemoryUsage(0, 300 * MB, 512 * MB, 1000 * MB));
        MemoryHealthIndicator indicator = new MemoryHealthIndicator(memoryBean, 0.9);

        // when
        Health health = indicator.health();

        // then
        assertEquals(Status.UP, health.getStatus());
        assertEquals(300L, health.getDetails().get("usedMb"));
        assertEquals("30.0%", health.getDetails().get("usage"));
        assertEquals("90.0%", health.getDetails().get("threshold"));
    }

    @Test
    void health_usageAboveThreshold_isDown() {
        // given
        when(memoryBean.getHeapMemoryUsage()).thenReturn(new MemoryUsage(0, 950 * MB, 1000 * MB, 1000 * MB));
        MemoryHealthIndicator indicator = new MemoryHealthIndicator(memoryBean, 0.9);

        // when
        Health health = indicator.health();

        // then
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("95.0%", health.getDetails().get("usage"));
    }

    @Test
    void health_undefinedMaxHeap_isUnknown() {
        // given
        when(memoryBean.getHeapMemoryUsage()).thenReturn(new MemoryUsage(0, 300 * MB, 512 * MB, -1));
        MemoryHealthIndicator indicator = new MemoryHealthIndicator(memoryBean, 0.9);

        // when
        Health health = indicator.health();

        // then
        assertEquals(Status.UNKNOWN, health.getStatus());
    }

    @Test
    void constructor_thresholdOutOfRange_throwsException() {
        // given
        double threshold = 1.5;

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new MemoryHealthIndicator(memoryBean, threshold));

        // then
        assertEquals("Max heap usage must be between 0 and 1", exception.getMessage());
    }
}
