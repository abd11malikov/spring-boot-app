package com.epam.gym.component;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * One application context for all component scenarios: workload publishing switched on, against an embedded
 * broker, with its own in-memory database.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:gym_crm_component;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.activemq.broker-url=vm://component?broker.persistent=false&broker.useJmx=false",
        "gym.workload.enabled=true"
})
public class CucumberSpringConfiguration {
}
