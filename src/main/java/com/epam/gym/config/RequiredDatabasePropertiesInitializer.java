package com.epam.gym.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

public class RequiredDatabasePropertiesInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String[] REQUIRED_PROPERTIES = {"DB_URL", "DB_USERNAME", "DB_PASSWORD"};
    static final Profiles PROFILES_WITHOUT_DEFAULTS = Profiles.of("stg | prod | docker");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();
        if (environment.acceptsProfiles(PROFILES_WITHOUT_DEFAULTS)) {
            environment.setRequiredProperties(REQUIRED_PROPERTIES);
        }
    }
}
