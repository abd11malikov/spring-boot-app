package com.epam.gym.monitoring.health;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionPoolHealthIndicatorTest {

    @Mock
    private HikariDataSource dataSource;

    @Mock
    private HikariPoolMXBean pool;

    @BeforeEach
    void setUp() {
        when(dataSource.getHikariPoolMXBean()).thenReturn(pool);
    }

    @Test
    void health_noWaitingRequests_isUpWithPoolDetails() {
        // given
        when(dataSource.getPoolName()).thenReturn("HikariPool-1");
        when(dataSource.getMaximumPoolSize()).thenReturn(10);
        when(pool.getActiveConnections()).thenReturn(3);
        when(pool.getIdleConnections()).thenReturn(7);
        when(pool.getTotalConnections()).thenReturn(10);
        when(pool.getThreadsAwaitingConnection()).thenReturn(0);

        // when
        Health health = new ConnectionPoolHealthIndicator(dataSource).health();

        // then
        assertEquals(Status.UP, health.getStatus());
        assertEquals(3, health.getDetails().get("active"));
        assertEquals(10, health.getDetails().get("max"));
        assertEquals(0, health.getDetails().get("waiting"));
    }

    @Test
    void health_requestsWaitingForConnection_isDown() {
        // given
        when(pool.getActiveConnections()).thenReturn(10);
        when(dataSource.getMaximumPoolSize()).thenReturn(10);
        when(pool.getThreadsAwaitingConnection()).thenReturn(4);

        // when
        Health health = new ConnectionPoolHealthIndicator(dataSource).health();

        // then
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Requests are waiting for a database connection", health.getDetails().get("reason"));
        assertEquals(4, health.getDetails().get("waiting"));
    }
}

class ConnectionPoolHealthIndicatorWithoutHikariTest {

    @Test
    void health_nonHikariDataSource_isUnknown() {
        // given
        DataSource plainDataSource = mock(DataSource.class);

        // when
        Health health = new ConnectionPoolHealthIndicator(plainDataSource).health();

        // then
        assertEquals(Status.UNKNOWN, health.getStatus());
    }
}
