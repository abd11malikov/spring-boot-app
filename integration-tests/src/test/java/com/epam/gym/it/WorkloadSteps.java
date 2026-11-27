package com.epam.gym.it;

import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks on the Trainer Workload service's side. Updates arrive asynchronously, so expectations are polled.
 */
public class WorkloadSteps {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(60);

    private final ScenarioContext context;

    public WorkloadSteps(ScenarioContext context) {
        this.context = context;
    }

    @Then("the Trainer Workload service shows {int} minutes for {string} in {int}\\/{int}")
    public void theWorkloadServiceShows(int minutes, String trainer, int month, int year) {
        String username = context.account(trainer).username();
        await().atMost(ASYNC_TIMEOUT).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
            Response response = workload("/api/trainer-workloads/" + username + "/years/" + year + "/months/" + month);
            assertEquals(200, response.statusCode(), response.asString());
            assertEquals(minutes, response.jsonPath().getInt("trainingSummaryDuration"));
        });
    }

    @Then("the Trainer Workload service has no workload for {string}")
    public void theWorkloadServiceHasNoWorkload(String trainer) {
        String username = context.account(trainer).username();
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(5))
                .until(() -> workload("/api/trainer-workloads/" + username).statusCode() == 404);
    }

    @Then("both services logged transaction {string}")
    public void bothServicesLoggedTransaction(String transactionId) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertTrue(Platform.logsOf(Platform.GYM_CRM).contains("[" + transactionId + "]"));
            assertTrue(Platform.logsOf(Platform.WORKLOAD).contains("[" + transactionId + "]"));
        });
    }

    @Then("the discovery server lists {string} and {string}")
    public void theDiscoveryServerLists(String first, String second) {
        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            String apps = RestAssured.given().baseUri(Platform.eurekaUrl()).accept("application/json")
                    .get("/eureka/apps").asString();
            assertTrue(apps.contains("\"" + first + "\""), apps);
            assertTrue(apps.contains("\"" + second + "\""), apps);
        });
    }

    private static Response workload(String path) {
        return RestAssured.given().baseUri(Platform.workloadUrl())
                .header("Authorization", Platform.serviceToken())
                .get(path);
    }
}
