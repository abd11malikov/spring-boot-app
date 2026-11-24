package com.epam.gym.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.MissingRequiredPropertiesException;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequiredPropertiesInitializerTest {

    private final RequiredPropertiesInitializer initializer = new RequiredPropertiesInitializer();

    @Test
    void prodProfile_withoutRequiredVariables_failsValidationNamingThem() {
        // given
        StaticApplicationContext context = contextWith(new MockEnvironment(), "prod");

        // when
        initializer.initialize(context);
        MissingRequiredPropertiesException exception = assertThrows(MissingRequiredPropertiesException.class,
                () -> context.getEnvironment().validateRequiredProperties());

        // then
        assertEquals("The following properties were declared as required but could not be resolved: "
                + "[DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, SERVICE_JWT_SECRET, EUREKA_URL]", exception.getMessage());
    }

    @Test
    void stgProfile_withAllRequiredVariables_passesValidation() {
        // given
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DB_URL", "jdbc:postgresql://db:5432/gym")
                .withProperty("DB_USERNAME", "gym")
                .withProperty("DB_PASSWORD", "secret")
                .withProperty("JWT_SECRET", "c2VjcmV0")
                .withProperty("SERVICE_JWT_SECRET", "c2VjcmV0")
                .withProperty("EUREKA_URL", "http://discovery-server:8761/eureka/");
        StaticApplicationContext context = contextWith(environment, "stg");

        // when
        initializer.initialize(context);

        // then
        assertDoesNotThrow(() -> context.getEnvironment().validateRequiredProperties());
    }

    @Test
    void dockerProfile_withoutDatabaseVariables_failsValidation() {
        // given
        StaticApplicationContext context = contextWith(new MockEnvironment(), "docker");

        // when
        initializer.initialize(context);

        // then
        assertThrows(MissingRequiredPropertiesException.class,
                () -> context.getEnvironment().validateRequiredProperties());
    }

    @Test
    void localProfile_requiresNothing() {
        // given
        StaticApplicationContext context = contextWith(new MockEnvironment(), "local");

        // when
        initializer.initialize(context);

        // then
        assertDoesNotThrow(() -> context.getEnvironment().validateRequiredProperties());
    }

    private StaticApplicationContext contextWith(MockEnvironment environment, String profile) {
        environment.setActiveProfiles(profile);
        StaticApplicationContext context = new StaticApplicationContext();
        context.setEnvironment(environment);
        return context;
    }
}
