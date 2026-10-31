package com.epam.gym.web.controller;

import com.epam.gym.web.auth.CredentialsArgumentResolver;
import com.epam.gym.web.config.WebConfig;
import com.epam.gym.web.error.GlobalExceptionHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class ControllerTestSupport {

    static final ObjectMapper OBJECT_MAPPER = WebConfig.createObjectMapper();

    private ControllerTestSupport() {
    }

    static MockMvc mockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CredentialsArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(OBJECT_MAPPER))
                .build();
    }

    static String basicAuth(String username, String password) {
        String token = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    static String json(Object body) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(body);
    }
}
