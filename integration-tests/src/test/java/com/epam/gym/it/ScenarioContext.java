package com.epam.gym.it;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * What one scenario remembers between steps (a new instance per scenario, injected by PicoContainer).
 */
public class ScenarioContext {

    public record Account(String username, String password, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private final Map<String, Account> accounts = new HashMap<>();
    private Response lastResponse;

    public void remember(String alias, Account account) {
        accounts.put(alias, account);
    }

    public Account account(String alias) {
        Account account = accounts.get(alias);
        if (account == null) {
            throw new IllegalStateException("No account registered as \"" + alias + "\" in this scenario");
        }
        return account;
    }

    public Response lastResponse() {
        return lastResponse;
    }

    public void setLastResponse(Response response) {
        this.lastResponse = response;
    }
}
