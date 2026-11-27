package com.epam.trainerworkload.component;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * What one scenario remembers between steps: trainers' names and the last REST response.
 */
@Component
@ScenarioScope
public class ScenarioState {

    public record TrainerName(String firstName, String lastName) {
    }

    private final Map<String, TrainerName> trainers = new HashMap<>();
    private MvcResult lastResult;

    public void rememberTrainer(String username, String firstName, String lastName) {
        trainers.put(username, new TrainerName(firstName, lastName));
    }

    public TrainerName trainer(String username) {
        return trainers.getOrDefault(username, new TrainerName("Unknown", "Trainer"));
    }

    public void setLastResult(MvcResult result) {
        this.lastResult = result;
    }

    public int lastStatus() {
        return lastResult.getResponse().getStatus();
    }

    public String lastBody() {
        try {
            return lastResult.getResponse().getContentAsString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
