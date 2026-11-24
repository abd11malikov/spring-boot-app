package com.epam.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServerApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void health_isUp() {
        // when
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    void registry_isServedOverEurekaApi() {
        // when
        ResponseEntity<String> response = restTemplate.getForEntity("/eureka/apps", String.class);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
