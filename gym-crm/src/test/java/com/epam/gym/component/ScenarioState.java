package com.epam.gym.component;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * What one scenario remembers between its steps: the registered accounts by alias and the last response.
 * A fresh instance per scenario keeps scenarios independent.
 */
@Component
@ScenarioScope
public class ScenarioState {

    public record Account(String username, String password, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private final Map<String, Account> accounts = new HashMap<>();
    private MvcResult lastResult;

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
