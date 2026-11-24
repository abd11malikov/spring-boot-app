package com.epam.gym.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/**
 * {@code stg}, {@code prod} and {@code docker} have no defaults for the database, signing keys, registry address
 * and message broker: startup fails and names every missing variable.
 */
public class RequiredPropertiesInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String[] REQUIRED_PROPERTIES = {
            "DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET", "SERVICE_JWT_SECRET", "EUREKA_URL",
            "ACTIVEMQ_URL", "ACTIVEMQ_USER", "ACTIVEMQ_PASSWORD"};
    static final Profiles PROFILES_WITHOUT_DEFAULTS = Profiles.of("stg | prod | docker");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();
        if (environment.acceptsProfiles(PROFILES_WITHOUT_DEFAULTS)) {
            environment.setRequiredProperties(REQUIRED_PROPERTIES);
        }
    }
}
