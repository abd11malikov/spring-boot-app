package com.epam.gym.component;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Component tests of Gym CRM, written as Cucumber scenarios ({@code features/component/*.feature}).
 * The whole service runs (REST API, security, database, JMS), with its outside world replaced by test doubles:
 * H2 instead of PostgreSQL and an embedded ActiveMQ broker, whose queue the scenarios read to check what Gym CRM
 * sends to the Trainer Workload service.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/component")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.epam.gym.component")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports/component.html, json:target/cucumber-reports/component.json")
class GymCrmComponentTest {
}
