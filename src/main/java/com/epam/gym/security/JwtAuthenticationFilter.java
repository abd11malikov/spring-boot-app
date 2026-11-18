package com.epam.gym.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <jwt>}. An invalid, expired or revoked
 * token leaves the request unauthenticated; the reason is kept for the 401 response.
 * <p>
 * Deliberately not a Spring bean, so it runs only inside the security filter chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    static final String TOKEN_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".error";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklist tokenBlacklist) {
        this.jwtService = jwtService;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            authenticate(request, header.substring(BEARER_PREFIX.length()).trim());
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String rawToken) {
        JwtToken token;
        try {
            token = jwtService.parse(rawToken);
        } catch (ExpiredJwtException e) {
            reject(request, "Token has expired");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            reject(request, "Invalid token");
            return;
        }
        if (tokenBlacklist.isRevoked(token.id())) {
            reject(request, "Token has been revoked by logout");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                token.username(), null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        authentication.setDetails(token);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        log.debug("Request authenticated by token of user {}", token.username());
    }

    private void reject(HttpServletRequest request, String reason) {
        log.debug("Bearer token rejected: {}", reason);
        SecurityContextHolder.clearContext();
        request.setAttribute(TOKEN_ERROR_ATTRIBUTE, reason);
    }
}
