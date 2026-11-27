package com.epam.gym.it;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Integration tests of the microservices, written as Cucumber scenarios ({@code features/integration/*.feature}).
 * {@link Platform} starts the real containers from compose.yml once for all scenarios (Docker needed).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/integration")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.epam.gym.it")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports/integration.html, json:target/cucumber-reports/integration.json")
class PlatformIntegrationTest {
}
