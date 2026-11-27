package com.epam.gym.it;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Gym CRM, used over HTTP like a real client.
 */
public class GymCrmSteps {

    private final ScenarioContext context;

    public GymCrmSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("a trainer {string} and a trainee {string} are registered in Gym CRM")
    public void aTrainerAndATraineeAreRegistered(String trainerAlias, String traineeAlias) {
        aTraineeIsRegistered(traineeAlias);
        Response types = gymCrm().header("Authorization", context.account(traineeAlias).bearer())
                .get("/api/training-types");
        int yogaId = types.jsonPath().getInt("find { it.trainingType == 'Yoga' }.id");
        context.remember(trainerAlias, registered(gymCrm().contentType(ContentType.JSON)
                .body("{\"firstName\":\"Integration\",\"lastName\":\"Coach\",\"specializationId\":" + yogaId + "}")
                .post("/api/trainers")));
    }

    @Given("a trainee {string} is registered in Gym CRM")
    public void aTraineeIsRegistered(String alias) {
        context.remember(alias, registered(gymCrm().contentType(ContentType.JSON)
                .body("{\"firstName\":\"Integration\",\"lastName\":\"Trainee\"}")
                .post("/api/trainees")));
    }

    @When("{string} adds a {int} minute training with {string} on {string}")
    public void addsATraining(String trainee, int minutes, String trainer, String date) {
        context.setLastResponse(addTraining(trainee, trainee, trainer, date, minutes, null));
    }

    @When("{string} adds a {int} minute training with {string} on {string} with transaction id {string}")
    public void addsATrainingWithTransactionId(String trainee, int minutes, String trainer, String date,
                                               String transactionId) {
        context.setLastResponse(addTraining(trainee, trainee, trainer, date, minutes, transactionId));
    }

    @Given("{string} added a {int} minute training with {string} on {string}")
    public void addedATraining(String trainee, int minutes, String trainer, String date) {
        addsATraining(trainee, minutes, trainer, date);
        assertEquals(200, context.lastResponse().statusCode(), context.lastResponse().asString());
    }

    @When("{string} adds a {int} minute training for {string} with {string} on {string}")
    public void addsATrainingForSomeoneElse(String caller, int minutes, String trainee, String trainer, String date) {
        context.setLastResponse(addTraining(caller, trainee, trainer, date, minutes, null));
    }

    @When("{string} deletes their profile")
    public void deletesTheirProfile(String alias) {
        ScenarioContext.Account account = context.account(alias);
        context.setLastResponse(gymCrm().header("Authorization", account.bearer())
                .delete("/api/trainees/" + account.username()));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertEquals(status, context.lastResponse().statusCode(), context.lastResponse().asString());
    }

    @Then("the error message is {string}")
    public void theErrorMessageIs(String message) {
        assertEquals(message, context.lastResponse().jsonPath().getString("message"));
    }

    @Then("the trainings of {string} in Gym CRM add up to {int} minutes")
    public void theTrainingsAddUpTo(String alias, int minutes) {
        ScenarioContext.Account account = context.account(alias);
        List<Integer> durations = gymCrm().header("Authorization", account.bearer())
                .get("/api/trainees/" + account.username() + "/trainings")
                .jsonPath().getList("trainingDuration", Integer.class);
        assertEquals(minutes, durations.stream().mapToInt(Integer::intValue).sum());
    }

    private Response addTraining(String caller, String trainee, String trainer, String date, int minutes,
                                 String transactionId) {
        RequestSpecification request = gymCrm()
                .header("Authorization", context.account(caller).bearer())
                .contentType(ContentType.JSON)
                .body("{\"traineeUsername\":\"" + context.account(trainee).username() + "\",\"trainerUsername\":\""
                        + context.account(trainer).username() + "\",\"trainingName\":\"Session\",\"trainingDate\":\""
                        + date + "\",\"trainingDuration\":" + minutes + "}");
        if (transactionId != null) {
            request.header("X-Transaction-Id", transactionId);
        }
        return request.post("/api/trainings");
    }

    private static ScenarioContext.Account registered(Response response) {
        assertEquals(201, response.statusCode(), response.asString());
        return new ScenarioContext.Account(response.jsonPath().getString("username"),
                response.jsonPath().getString("password"), response.jsonPath().getString("token"));
    }

    private static RequestSpecification gymCrm() {
        return RestAssured.given().baseUri(Platform.gymCrmUrl());
    }
}
