package com.epam.gym.monitoring.health;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class ConnectionPoolHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public ConnectionPoolHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        if (!(dataSource instanceof HikariDataSource hikari) || hikari.getHikariPoolMXBean() == null) {
            return Health.unknown().withDetail("reason", "Connection pool is not available").build();
        }
        HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
        int waiting = pool.getThreadsAwaitingConnection();
        Health.Builder builder = waiting > 0 ? Health.down() : Health.up();
        if (waiting > 0) {
            builder.withDetail("reason", "Requests are waiting for a database connection");
        }
        return builder
                .withDetail("pool", String.valueOf(hikari.getPoolName()))
                .withDetail("active", pool.getActiveConnections())
                .withDetail("idle", pool.getIdleConnections())
                .withDetail("total", pool.getTotalConnections())
                .withDetail("max", hikari.getMaximumPoolSize())
                .withDetail("waiting", waiting)
                .build();
    }
}
