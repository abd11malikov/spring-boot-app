package com.epam.trainerworkload.component;

import com.epam.trainerworkload.ServiceTokens;
import com.epam.trainerworkload.model.ActionType;
import com.epam.trainerworkload.service.TrainerWorkloadService;
import com.epam.trainerworkload.service.TrainerWorkloadUpdate;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The read-only REST API, called like another service would, with or without a service token.
 */
public class ApiSteps {

    private static final String URL = "/api/trainer-workloads";

    private final MockMvc mockMvc;
    private final TrainerWorkloadService service;
    private final ScenarioState state;

    public ApiSteps(MockMvc mockMvc, TrainerWorkloadService service, ScenarioState state) {
        this.mockMvc = mockMvc;
        this.service = service;
        this.state = state;
    }

    @Given("trainer {string} named {string} {string} has trained {int} minutes on {string}")
    public void trainerHasTrained(String username, String firstName, String lastName, int minutes, String date) {
        service.apply(new TrainerWorkloadUpdate(username, firstName, lastName, true, LocalDate.parse(date), minutes,
                ActionType.ADD));
    }

    @When("the summary of {string} is requested with a service token")
    public void theSummaryIsRequested(String username) {
        perform(get(URL + "/" + username), ServiceTokens.valid());
    }

    @When("the summary of {string} is requested without a token")
    public void theSummaryIsRequestedWithoutToken(String username) {
        perform(get(URL + "/" + username), null);
    }

    @When("the workload of {string} in {int}\\/{int} is requested with a service token")
    public void theMonthlyWorkloadIsRequested(String username, int month, int year) {
        perform(get(URL + "/" + username + "/years/" + year + "/months/" + month), ServiceTokens.valid());
    }

    @When("trainers named {string} {string} are searched with a service token")
    public void trainersAreSearched(String firstName, String lastName) {
        perform(get(URL).param("firstName", firstName).param("lastName", lastName), ServiceTokens.valid());
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertEquals(status, state.lastStatus(), state::lastBody);
    }

    @Then("the summary shows {int} minutes in {int}\\/{int}")
    public void theSummaryShows(int minutes, int month, int year) {
        List<Integer> found = JsonPath.read(state.lastBody(),
                "$.years[?(@.year == " + year + ")].months[?(@.month == " + month + ")].trainingSummaryDuration");
        assertEquals(List.of(minutes), found);
    }

    @Then("the monthly workload is {int} minutes")
    public void theMonthlyWorkloadIs(int minutes) {
        assertEquals(minutes, (int) JsonPath.read(state.lastBody(), "$.trainingSummaryDuration"));
    }

    @Then("the search finds {string}")
    public void theSearchFinds(String username) {
        assertEquals(List.of(username), JsonPath.read(state.lastBody(), "$[*].trainerUsername"));
    }

    private void perform(MockHttpServletRequestBuilder request, String authorization) {
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        try {
            state.setLastResult(mockMvc.perform(request).andReturn());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
