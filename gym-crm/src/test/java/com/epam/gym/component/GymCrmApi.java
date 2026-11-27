package com.epam.gym.component;

import com.epam.gym.web.logging.TransactionIdFilter;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * The Gym CRM REST API as the scenarios use it, through MockMvc (the full filter chain, security included).
 */
@Component
public class GymCrmApi {

    private final MockMvc mockMvc;

    public GymCrmApi(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public MvcResult registerTrainee(String body) {
        return perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    public MvcResult registerTrainer(String body) {
        return perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    public MvcResult login(String username, String password) {
        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return perform(get("/api/auth/login").header(HttpHeaders.AUTHORIZATION, "Basic " + credentials));
    }

    public MvcResult logout(String bearer) {
        return perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer));
    }

    public MvcResult getTrainee(String bearer, String username) {
        return perform(authorized(get("/api/trainees/" + username), bearer));
    }

    public MvcResult updateTrainee(String bearer, String username, String body) {
        return perform(authorized(put("/api/trainees/" + username), bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    public MvcResult deleteTrainee(String bearer, String username) {
        return perform(authorized(delete("/api/trainees/" + username), bearer));
    }

    public MvcResult addTraining(String bearer, String body, String transactionId) {
        MockHttpServletRequestBuilder request = authorized(post("/api/trainings"), bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body);
        if (transactionId != null) {
            request.header(TransactionIdFilter.HEADER, transactionId);
        }
        return perform(request);
    }

    public MvcResult trainingTypes(String bearer) {
        MockHttpServletRequestBuilder request = get("/api/training-types");
        return perform(bearer == null ? request : authorized(request, bearer));
    }

    public long trainingTypeId(String bearer, String name) throws Exception {
        String json = trainingTypes(bearer).getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(json, "$[?(@.trainingType == '" + name + "')].id");
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("No training type " + name);
        }
        return ids.get(0);
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String bearer) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer);
    }

    private MvcResult perform(MockHttpServletRequestBuilder request) {
        try {
            return mockMvc.perform(request).andReturn();
        } catch (Exception e) {
            throw new IllegalStateException("Request failed: " + e.getMessage(), e);
        }
    }
}
