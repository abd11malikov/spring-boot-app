package com.epam.gym.web.controller;

import com.epam.gym.TestObjectMapper;
import com.epam.gym.security.JwtService;
import com.epam.gym.web.error.GlobalExceptionHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Duration;

/**
 * Controllers are tested without the security filter chain (see the security tests for that):
 * {@link #asUser} plays the role of the JWT filter by putting an authenticated user into the security context.
 */
final class ControllerTestSupport {

    static final ObjectMapper OBJECT_MAPPER = TestObjectMapper.create();
    static final JwtService JWT_SERVICE = new JwtService(
            "dGVzdC1vbmx5LWd5bS1jcm0tand0LXNlY3JldC1rZXktMjU2LWJpdHM=", Duration.ofHours(1), Clock.systemUTC());

    private ControllerTestSupport() {
    }

    static MockMvc mockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(OBJECT_MAPPER))
                .alwaysDo(result -> SecurityContextHolder.clearContext())
                .build();
    }

    static RequestPostProcessor asUser(String username) {
        return request -> {
            Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                    username, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setUserPrincipal(authentication);
            return request;
        };
    }

    static String json(Object body) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(body);
    }
}
