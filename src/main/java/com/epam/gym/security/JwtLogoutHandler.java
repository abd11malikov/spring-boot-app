package com.epam.gym.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

/**
 * Revokes the bearer token the logout request was authenticated with.
 */
public class JwtLogoutHandler implements LogoutHandler {
    private static final Logger log = LoggerFactory.getLogger(JwtLogoutHandler.class);

    private final TokenBlacklist tokenBlacklist;

    public JwtLogoutHandler(TokenBlacklist tokenBlacklist) {
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtToken token) {
            tokenBlacklist.revoke(token);
            log.info("User {} logged out", token.username());
        }
    }
}
