package com.epam.gym.component;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Then;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResponseSteps {

    private final ScenarioState state;

    public ResponseSteps(ScenarioState state) {
        this.state = state;
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertEquals(status, state.lastStatus(), () -> "Response body: " + state.lastBody());
    }

    @Then("the error message is {string}")
    public void theErrorMessageIs(String message) {
        assertEquals(message, JsonPath.read(state.lastBody(), "$.message"));
    }

    @Then("the response names the invalid field {string}")
    public void theResponseNamesTheInvalidField(String field) {
        List<String> fields = JsonPath.read(state.lastBody(), "$.violations[*].field");
        assertTrue(fields.contains(field), () -> "Violations: " + fields);
    }
}
