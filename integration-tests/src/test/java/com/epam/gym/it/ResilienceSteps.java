package com.epam.gym.it;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stops and restarts containers of the platform. Anything a scenario stopped is started again afterwards, even
 * if the scenario failed, so later scenarios find the platform complete.
 */
public class ResilienceSteps {

    private final Set<String> stopped = new LinkedHashSet<>();

    @Given("the Trainer Workload service is stopped")
    public void theWorkloadServiceIsStopped() {
        stop(Platform.WORKLOAD);
    }

    @When("the Trainer Workload service is started again")
    public void theWorkloadServiceIsStartedAgain() {
        start(Platform.WORKLOAD);
    }

    @Given("the message broker is stopped")
    public void theMessageBrokerIsStopped() {
        stop(Platform.BROKER);
    }

    @When("the message broker is started again")
    public void theMessageBrokerIsStartedAgain() {
        start(Platform.BROKER);
    }

    @After
    public void restartAnythingStillStopped() {
        new LinkedHashSet<>(stopped).forEach(this::start);
    }

    private void stop(String service) {
        Platform.stopService(service);
        stopped.add(service);
    }

    private void start(String service) {
        Platform.startService(service);
        stopped.remove(service);
    }
}
