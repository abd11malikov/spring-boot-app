package com.epam.trainerworkload.component;

import com.epam.trainerworkload.MongoTestContainer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MongoTestContainer.class)
@TestPropertySource(properties =
        "spring.activemq.broker-url=vm://workload-component?broker.persistent=false&broker.useJmx=false")
public class CucumberSpringConfiguration {
}
