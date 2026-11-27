package com.epam.gym.component;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registration, login and logout.
 */
public class AccountSteps {

    private final GymCrmApi api;
    private final ScenarioState state;

    public AccountSteps(GymCrmApi api, ScenarioState state) {
        this.api = api;
        this.state = state;
    }

    @Given("a trainee {string} registered as {string} {string}")
    public void aTraineeRegisteredAs(String alias, String firstName, String lastName) {
        state.remember(alias, registered(api.registerTrainee(person(firstName, lastName))));
    }

    @Given("a trainer {string} registered as {string} {string} with specialization {string}")
    public void aTrainerRegisteredAs(String alias, String firstName, String lastName, String specialization) {
        state.remember(alias, registered(api.registerTrainer(trainer(firstName, lastName, specializationId(specialization)))));
    }

    @When("a trainee registers with first name {string} and last name {string}")
    public void aTraineeRegisters(String firstName, String lastName) {
        state.setLastResult(api.registerTrainee(person(firstName, lastName)));
    }

    @When("a trainee registers with the body {}")
    public void aTraineeRegistersWithTheBody(String body) {
        state.setLastResult(api.registerTrainee(body));
    }

    @When("a trainer registers with first name {string} and last name {string} and specialization {string}")
    public void aTrainerRegisters(String firstName, String lastName, String specialization) {
        state.setLastResult(api.registerTrainer(trainer(firstName, lastName, specializationId(specialization))));
    }

    @When("a trainer registers with first name {string} and last name {string} and specialization id {long}")
    public void aTrainerRegistersWithSpecializationId(String firstName, String lastName, long specializationId) {
        state.setLastResult(api.registerTrainer(trainer(firstName, lastName, specializationId)));
    }

    @When("{string} logs in with the right password")
    public void logsInWithTheRightPassword(String alias) {
        ScenarioState.Account account = state.account(alias);
        state.setLastResult(api.login(account.username(), account.password()));
    }

    @When("{string} logs in with the password {string}")
    public void logsInWithThePassword(String alias, String password) {
        state.setLastResult(api.login(state.account(alias).username(), password));
    }

    @Given("{string} failed to log in {int} times")
    public void failedToLogIn(String alias, int times) {
        for (int i = 0; i < times; i++) {
            assertEquals(401, api.login(state.account(alias).username(), "wrong-password").getResponse().getStatus());
        }
    }

    @When("{string} logs out")
    public void logsOut(String alias) {
        state.setLastResult(api.logout(state.account(alias).bearer()));
    }

    @When("an anonymous user requests the training types")
    public void anAnonymousUserRequestsTheTrainingTypes() {
        state.setLastResult(api.trainingTypes(null));
    }

    @Then("the response has a username starting with {string}")
    public void theResponseHasAUsernameStartingWith(String prefix) {
        String username = JsonPath.read(state.lastBody(), "$.username");
        assertTrue(username.startsWith(prefix), () -> "Username: " + username);
    }

    @Then("the response has a {int} character password")
    public void theResponseHasAPasswordOfLength(int length) {
        assertEquals(length, JsonPath.<String>read(state.lastBody(), "$.password").length());
    }

    @Then("the new username is the username of {string} followed by a serial number")
    public void theNewUsernameHasASerialNumber(String alias) {
        String username = JsonPath.read(state.lastBody(), "$.username");
        assertTrue(username.matches(java.util.regex.Pattern.quote(state.account(alias).username()) + "\\d+"),
                () -> "Username: " + username);
    }

    @Then("the response contains a bearer token")
    public void theResponseContainsABearerToken() {
        assertEquals("Bearer", JsonPath.read(state.lastBody(), "$.tokenType"));
    }

    @Then("the token from the response opens the trainee's profile")
    public void theTokenOpensTheTraineesProfile() {
        String username = JsonPath.read(state.lastBody(), "$.username");
        assertOpensProfile(JsonPath.read(state.lastBody(), "$.token"), username);
    }

    @Then("the token from the response opens the profile of {string}")
    public void theTokenOpensTheProfileOf(String alias) {
        assertOpensProfile(JsonPath.read(state.lastBody(), "$.token"), state.account(alias).username());
    }

    @Then("the old token of {string} is rejected with {string}")
    public void theOldTokenIsRejected(String alias, String message) {
        ScenarioState.Account account = state.account(alias);
        state.setLastResult(api.getTrainee(account.bearer(), account.username()));
        assertEquals(401, state.lastStatus());
        assertEquals(message, JsonPath.read(state.lastBody(), "$.message"));
    }

    private void assertOpensProfile(String token, String username) {
        assertEquals(200, api.getTrainee("Bearer " + token, username).getResponse().getStatus());
    }

    private long specializationId(String name) {
        try {
            ScenarioState.Account reader = registered(api.registerTrainee(person("Type", "Reader")));
            return api.trainingTypeId(reader.bearer(), name);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ScenarioState.Account registered(MvcResult result) {
        try {
            assertEquals(201, result.getResponse().getStatus(), result.getResponse().getContentAsString());
            String json = result.getResponse().getContentAsString();
            return new ScenarioState.Account(JsonPath.read(json, "$.username"), JsonPath.read(json, "$.password"),
                    JsonPath.read(json, "$.token"));
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String person(String firstName, String lastName) {
        return "{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\"}";
    }

    private static String trainer(String firstName, String lastName, long specializationId) {
        return "{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\",\"specializationId\":"
                + specializationId + "}";
    }
}
