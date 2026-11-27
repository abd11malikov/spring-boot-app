package com.epam.gym.component;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TrainingSteps {

    private final GymCrmApi api;
    private final ScenarioState state;
    private final WorkloadQueue workloadQueue;

    public TrainingSteps(GymCrmApi api, ScenarioState state, WorkloadQueue workloadQueue) {
        this.api = api;
        this.state = state;
        this.workloadQueue = workloadQueue;
    }

    @When("{string} adds a {int} minute training with {string} on {string} with transaction id {string}")
    public void addsATrainingWithTransactionId(String trainee, int minutes, String trainer, String date,
                                               String transactionId) {
        state.setLastResult(api.addTraining(state.account(trainee).bearer(),
                training(trainee, trainer, date, minutes), transactionId));
    }

    @When("{string} adds a {int} minute training with {string} on {string}")
    public void addsATraining(String trainee, int minutes, String trainer, String date) {
        state.setLastResult(api.addTraining(state.account(trainee).bearer(),
                training(trainee, trainer, date, minutes), null));
    }

    @Given("{string} added a {int} minute training with {string} on {string}")
    public void addedATraining(String trainee, int minutes, String trainer, String date) {
        addsATraining(trainee, minutes, trainer, date);
        assertEquals(200, state.lastStatus(), state::lastBody);
        // a precondition: its ADD message is not what the scenario checks
        workloadQueue.drain();
    }

    @When("{string} adds a {int} minute training for {string} with {string} on {string}")
    public void addsATrainingForSomeoneElse(String caller, int minutes, String trainee, String trainer, String date) {
        state.setLastResult(api.addTraining(state.account(caller).bearer(),
                training(trainee, trainer, date, minutes), null));
    }

    @When("{string} deletes their profile")
    public void deletesTheirProfile(String alias) {
        ScenarioState.Account account = state.account(alias);
        state.setLastResult(api.deleteTrainee(account.bearer(), account.username()));
    }

    private String training(String trainee, String trainer, String date, int minutes) {
        return "{\"traineeUsername\":\"" + state.account(trainee).username() + "\",\"trainerUsername\":\""
                + state.account(trainer).username() + "\",\"trainingName\":\"Session\",\"trainingDate\":\"" + date
                + "\",\"trainingDuration\":" + minutes + "}";
    }
}
