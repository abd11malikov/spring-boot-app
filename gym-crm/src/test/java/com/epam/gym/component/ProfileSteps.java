package com.epam.gym.component;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProfileSteps {

    private final GymCrmApi api;
    private final ScenarioState state;

    public ProfileSteps(GymCrmApi api, ScenarioState state) {
        this.api = api;
        this.state = state;
    }

    @When("{string} changes their name to {string} {string} and address to {string}")
    public void changesTheirProfile(String alias, String firstName, String lastName, String address) {
        ScenarioState.Account account = state.account(alias);
        state.setLastResult(api.updateTrainee(account.bearer(), account.username(),
                profile(firstName, lastName, address)));
    }

    @When("{string} tries to change the profile of {string}")
    public void triesToChangeTheProfileOf(String alias, String ownerAlias) {
        state.setLastResult(api.updateTrainee(state.account(alias).bearer(), state.account(ownerAlias).username(),
                profile("Hacked", "Name", "Nowhere")));
    }

    @When("{string} views the profile of {string}")
    public void viewsTheProfileOf(String alias, String ownerAlias) {
        state.setLastResult(api.getTrainee(state.account(alias).bearer(), state.account(ownerAlias).username()));
    }

    @When("{string} views the trainee profile {string}")
    public void viewsTheTraineeProfile(String alias, String username) {
        state.setLastResult(api.getTrainee(state.account(alias).bearer(), username));
    }

    @Then("the profile of {string} shows first name {string} and address {string}")
    public void theProfileShows(String alias, String firstName, String address) {
        ScenarioState.Account account = state.account(alias);
        state.setLastResult(api.getTrainee(account.bearer(), account.username()));
        assertEquals(firstName, JsonPath.read(state.lastBody(), "$.firstName"));
        assertEquals(address, JsonPath.read(state.lastBody(), "$.address"));
    }

    private static String profile(String firstName, String lastName, String address) {
        return "{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\",\"address\":\"" + address
                + "\",\"isActive\":true}";
    }
}
