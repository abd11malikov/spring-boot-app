package com.epam.gym.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

/**
 * 200 after a successful logout; 401 when the request carried no valid bearer token to log out.
 */
public class RestLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AuthenticationEntryPoint entryPoint;

    public RestLogoutSuccessHandler(AuthenticationEntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        if (authentication == null) {
            entryPoint.commence(request, response,
                    new InsufficientAuthenticationException("No valid bearer token to log out"));
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
