package com.epam.gym.security;

import com.epam.gym.monitoring.metrics.GymMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Feeds the username/password checks done by Spring Security into the brute force protection and metrics.
 */
@Component
public class AuthenticationEventsListener {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationEventsListener.class);

    private final LoginAttemptService loginAttemptService;
    private final GymMetrics metrics;

    public AuthenticationEventsListener(LoginAttemptService loginAttemptService, GymMetrics metrics) {
        this.loginAttemptService = loginAttemptService;
        this.metrics = metrics;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        loginAttemptService.loginSucceeded(username);
        metrics.authenticationSucceeded();
        log.debug("User {} authenticated", username);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        metrics.authenticationFailed();
        log.warn("Authentication failed for username {}: {}", username, event.getException().getMessage());
        if (event instanceof AuthenticationFailureBadCredentialsEvent) {
            loginAttemptService.loginFailed(username);
        }
    }
}
