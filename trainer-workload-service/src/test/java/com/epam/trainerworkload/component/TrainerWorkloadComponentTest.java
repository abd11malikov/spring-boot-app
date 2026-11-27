package com.epam.trainerworkload.component;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Component tests of the Trainer Workload service, written as Cucumber scenarios
 * ({@code features/component/*.feature}). The whole service runs (JMS listener, REST API, security, MongoDB);
 * Gym CRM is played by the scenarios, which put messages on an embedded ActiveMQ broker. MongoDB is real, in a
 * container (Testcontainers), so Docker is needed.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/component")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.epam.trainerworkload.component")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports/component.html, json:target/cucumber-reports/component.json")
class TrainerWorkloadComponentTest {
}
