package com.epam.gym.monitoring.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

@Component
public class MemoryHealthIndicator implements HealthIndicator {

    private static final long MEGABYTE = 1024 * 1024;

    private final MemoryMXBean memoryBean;
    private final double maxHeapUsage;

    @Autowired
    public MemoryHealthIndicator(@Value("${gym.health.memory.max-heap-usage}") double maxHeapUsage) {
        this(ManagementFactory.getMemoryMXBean(), maxHeapUsage);
    }

    MemoryHealthIndicator(MemoryMXBean memoryBean, double maxHeapUsage) {
        if (maxHeapUsage <= 0 || maxHeapUsage > 1) {
            throw new IllegalArgumentException("Max heap usage must be between 0 and 1");
        }
        this.memoryBean = memoryBean;
        this.maxHeapUsage = maxHeapUsage;
    }

    @Override
    public Health health() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        if (heap.getMax() <= 0) {
            return Health.unknown().withDetail("reason", "Maximum heap size is not defined").build();
        }
        double usage = (double) heap.getUsed() / heap.getMax();
        Health.Builder builder = usage > maxHeapUsage ? Health.down() : Health.up();
        return builder
                .withDetail("usedMb", heap.getUsed() / MEGABYTE)
                .withDetail("maxMb", heap.getMax() / MEGABYTE)
                .withDetail("usage", percent(usage))
                .withDetail("threshold", percent(maxHeapUsage))
                .build();
    }

    private String percent(double fraction) {
        return Math.round(fraction * 1000) / 10.0 + "%";
    }
}
