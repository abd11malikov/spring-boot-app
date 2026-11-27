package com.epam.trainerworkload;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A real MongoDB (same major version as in compose.yml) in a container. {@link ServiceConnection} points
 * Spring Data MongoDB at it, so tests need Docker but no MongoDB installation.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MongoTestContainer {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:8.0"));
    }
}
