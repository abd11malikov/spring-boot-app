package com.epam.gym.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Answers unauthenticated requests with a JSON error: 429 for a user blocked by the brute force
 * protection, 401 with a {@code WWW-Authenticate} challenge otherwise.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String INVALID_CREDENTIALS = "Invalid username or password";
    static final String BLOCKED = "User is temporarily blocked after too many unsuccessful logins, try again later";

    private final SecurityErrorWriter errorWriter;
    private final LoginAttemptService loginAttemptService;
    private final String challenge;
    private final String missingCredentialsMessage;

    public RestAuthenticationEntryPoint(SecurityErrorWriter errorWriter,
                                        LoginAttemptService loginAttemptService,
                                        String challenge,
                                        String missingCredentialsMessage) {
        this.errorWriter = errorWriter;
        this.loginAttemptService = loginAttemptService;
        this.challenge = challenge;
        this.missingCredentialsMessage = missingCredentialsMessage;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        if (exception instanceof LockedException) {
            response.setHeader(HttpHeaders.RETRY_AFTER,
                    String.valueOf(loginAttemptService.getBlockDuration().toSeconds()));
            errorWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, BLOCKED);
            return;
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge);
        errorWriter.write(request, response, HttpStatus.UNAUTHORIZED, messageFor(request, exception));
    }

    private String messageFor(HttpServletRequest request, AuthenticationException exception) {
        if (exception instanceof BadCredentialsException) {
            return INVALID_CREDENTIALS;
        }
        Object tokenError = request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE);
        return tokenError != null ? tokenError.toString() : missingCredentialsMessage;
    }
}
