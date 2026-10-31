package com.epam.gym.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProfilePropertiesTest {

    private static final List<String> ENVIRONMENTS = List.of("local", "dev", "stg", "prod");

    @Test
    void everyEnvironment_definesItsOwnDatabaseSettings() throws IOException {
        // given
        Set<String> poolSizes = new HashSet<>();

        // when
        for (String environment : ENVIRONMENTS) {
            Properties properties = load(environment);
            assertNotNull(properties.getProperty("spring.datasource.url"), environment + " has no database URL");
            assertNotNull(properties.getProperty("spring.datasource.username"), environment + " has no username");
            assertNotNull(properties.getProperty("spring.datasource.password"), environment + " has no password");
            poolSizes.add(properties.getProperty("spring.datasource.hikari.maximum-pool-size"));
        }

        // then
        assertEquals(ENVIRONMENTS.size(), poolSizes.size(), "Each environment sizes its pool differently");
    }

    @Test
    void localAndDev_useDifferentDatabases() throws IOException {
        // given
        Properties local = load("local");
        Properties dev = load("dev");

        // when
        String localUrl = local.getProperty("spring.datasource.url");
        String devUrl = dev.getProperty("spring.datasource.url");

        // then
        assertEquals("jdbc:postgresql://localhost:5432/gym_crm", localUrl);
        assertEquals("${DB_URL:jdbc:postgresql://localhost:5432/gym_crm_dev}", devUrl);
    }

    @Test
    void stgAndProd_takeDatabaseSettingsOnlyFromEnvironment() throws IOException {
        // given
        List<String> keys = List.of("spring.datasource.url", "spring.datasource.username", "spring.datasource.password");

        for (String environment : List.of("stg", "prod")) {
            // when
            Properties properties = load(environment);

            // then
            for (String key : keys) {
                String value = properties.getProperty(key);
                assertFalse(value.contains(":"), environment + " must not have a default for " + key + ": " + value);
            }
        }
    }

    @Test
    void prod_disablesSwaggerAndHealthDetails() throws IOException {
        // given
        Properties prod = load("prod");

        // when
        String apiDocs = prod.getProperty("springdoc.api-docs.enabled");
        String healthDetails = prod.getProperty("management.endpoint.health.show-details");

        // then
        assertEquals("false", apiDocs);
        assertEquals("never", healthDetails);
    }

    private Properties load(String environment) throws IOException {
        return PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-" + environment + ".properties"));
    }
}
