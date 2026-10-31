package com.epam.gym.web.config;

import com.epam.gym.web.logging.TransactionIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebContextTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getTrainingTypes_throughRealContexts_returnsSeededTypes() throws Exception {
        // given
        String authorization = registerTraineeAndGetAuthorization();

        // when
        ResultActions result = mockMvc.perform(get("/api/training-types")
                .header(HttpHeaders.AUTHORIZATION, authorization));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].trainingType").value("Cardio"))
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    void getTrainingTypes_withoutCredentials_returns401JsonWithChallenge() throws Exception {
        // given
        String url = "/api/training-types";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"gym-crm\""))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void errorResponse_carriesSameTransactionIdAsResponseHeader() throws Exception {
        // given
        String incomingId = "client-trace-7";

        // when
        ResultActions result = mockMvc.perform(get("/api/training-types")
                .header(TransactionIdFilter.HEADER, incomingId));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(TransactionIdFilter.HEADER, incomingId))
                .andExpect(jsonPath("$.transactionId").value(incomingId));
    }

    @Test
    void unknownUrl_returns404Json() throws Exception {
        // given
        String url = "/api/does-not-exist";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No endpoint GET /api/does-not-exist"));
    }

    @Test
    void apiDocs_areServedAsOpenApiJson() throws Exception {
        // given
        String url = "/v3/api-docs";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.info.title").value("Gym CRM REST API"))
                .andExpect(jsonPath("$.paths['/api/training-types']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"));
    }

    @Test
    void apiDocs_documentErrorResponsesWithErrorSchema() throws Exception {
        // given
        String url = "/v3/api-docs";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(jsonPath("$.paths['/api/trainees'].post.responses['400'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath("$.paths['/api/trainees'].post.responses['201'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/RegistrationResponse"))
                .andExpect(jsonPath("$.paths['/api/training-types'].get.responses['500']").exists());
    }

    @Test
    void swaggerUi_isServed() throws Exception {
        // given
        String url = "/swagger-ui/index.html";

        // when
        ResultActions result = mockMvc.perform(get(url));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    void jsonDates_areWrittenAsIsoStrings() throws Exception {
        // given
        LocalDate date = LocalDate.of(2026, 3, 10);

        // when
        String json = objectMapper.writeValueAsString(date);

        // then
        assertEquals("\"2026-03-10\"", json);
    }

    private String registerTraineeAndGetAuthorization() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/trainees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Web\",\"lastName\":\"Tester\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        var credentials = objectMapper.readTree(registration.getResponse().getContentAsString());
        String token = credentials.get("username").asText() + ":" + credentials.get("password").asText();
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }
}
